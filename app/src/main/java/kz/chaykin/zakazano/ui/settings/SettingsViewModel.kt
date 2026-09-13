package kz.chaykin.zakazano.ui.settings

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.data.backup.BackupManager
import kz.chaykin.zakazano.data.prefs.Settings
import kz.chaykin.zakazano.data.prefs.SettingsStore
import kz.chaykin.zakazano.data.prefs.SyncState
import kz.chaykin.zakazano.data.prefs.ThemeMode
import kz.chaykin.zakazano.data.sync.DriveAccess
import kz.chaykin.zakazano.data.sync.DriveAuth
import kz.chaykin.zakazano.data.sync.DriveSync
import kz.chaykin.zakazano.model.Currency
import kz.chaykin.zakazano.ui.appContainer

/** Одноразовые события: их нельзя держать в состоянии, иначе снекбар повторится при повороте. */
sealed interface BackupEvent {
    data object Exported : BackupEvent
    data class ExportFailed(val reason: String) : BackupEvent
    data class Imported(val venues: Int, val items: Int) : BackupEvent
    data class ImportFailed(val reason: String) : BackupEvent

    /** Экран согласия Google показывается только из активити, отсюда — только просьба. */
    data class DriveConsentNeeded(val intent: PendingIntent) : BackupEvent
    data object DriveConnected : BackupEvent
    data object DriveDisconnected : BackupEvent
    data object DriveUploaded : BackupEvent
    data class DriveRestored(val venues: Int, val items: Int) : BackupEvent
    data class DriveFailed(val reason: String) : BackupEvent
}

/** Что именно хотел сделать пользователь — нужно помнить, пока он проходит согласие Google. */
private enum class DriveAction { CONNECT, UPLOAD, RESTORE }

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val backupManager: BackupManager,
    private val driveAuth: DriveAuth,
    private val driveSync: DriveSync,
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = Settings(),
    )

    val sync: StateFlow<SyncState> = settingsStore.sync.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SyncState(),
    )

    private val _busy = MutableStateFlow(false)

    /** Пока идёт разговор с Диском, кнопки заблокированы: два обмена подряд только навредят. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val eventChannel = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = eventChannel.receiveAsFlow()

    private var pendingAction: DriveAction? = null

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }

    fun setCurrency(currency: Currency) =
        viewModelScope.launch { settingsStore.setCurrency(currency) }

    fun export(target: Uri) = viewModelScope.launch {
        runCatching { backupManager.export(target) }
            .onSuccess { eventChannel.send(BackupEvent.Exported) }
            .onFailure { eventChannel.send(BackupEvent.ExportFailed(it.readableMessage())) }
    }

    fun import(source: Uri) = viewModelScope.launch {
        runCatching { backupManager.import(source) }
            .onSuccess { eventChannel.send(BackupEvent.Imported(it.venueCount, it.itemCount)) }
            .onFailure { eventChannel.send(BackupEvent.ImportFailed(it.readableMessage())) }
    }

    fun connectDrive() = startDriveAction(DriveAction.CONNECT)

    fun uploadToDrive() = startDriveAction(DriveAction.UPLOAD)

    fun restoreFromDrive() = startDriveAction(DriveAction.RESTORE)

    /** Возврат с экрана согласия: продолжаем ровно то, что просили до него. */
    fun onDriveConsent(data: Intent?) = viewModelScope.launch {
        val action = pendingAction ?: DriveAction.CONNECT
        pendingAction = null
        _busy.value = true
        try {
            when (val access = driveAuth.fromConsent(data)) {
                is DriveAccess.Granted -> perform(action, access)
                is DriveAccess.NeedsConsent -> fail("Google так и не подтвердил доступ")
                is DriveAccess.Failed -> fail(access.message)
            }
        } finally {
            _busy.value = false
        }
    }

    fun setAutoDaily(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setAutoDaily(enabled)
        driveSync.setDailyUpload(enabled)
    }

    fun disconnectDrive() = viewModelScope.launch {
        _busy.value = true
        try {
            driveSync.setDailyUpload(false)
            // Токен выбрасываем из кэша Play-сервисов, иначе «отключение» ничего не меняет.
            (driveAuth.request() as? DriveAccess.Granted)?.let { driveAuth.forget(it.token) }
            settingsStore.clearDrive()
            eventChannel.send(BackupEvent.DriveDisconnected)
        } finally {
            _busy.value = false
        }
    }

    private fun startDriveAction(action: DriveAction) = viewModelScope.launch {
        _busy.value = true
        try {
            when (val access = driveAuth.request()) {
                is DriveAccess.Granted -> perform(action, access)

                is DriveAccess.NeedsConsent -> {
                    pendingAction = action
                    eventChannel.send(BackupEvent.DriveConsentNeeded(access.intent))
                }

                is DriveAccess.Failed -> fail(access.message)
            }
        } finally {
            _busy.value = false
        }
    }

    private suspend fun perform(action: DriveAction, access: DriveAccess.Granted) {
        // Почта нужна только чтобы показать, куда уходят копии; спрашиваем её один раз.
        settingsStore.setDriveConnected(
            sync.value.accountEmail ?: driveSync.accountEmail(access.token),
        )

        when (action) {
            DriveAction.CONNECT -> eventChannel.send(BackupEvent.DriveConnected)

            DriveAction.UPLOAD -> runCatching { driveSync.upload(access.token) }
                .onSuccess {
                    settingsStore.setSyncSucceeded(it)
                    eventChannel.send(BackupEvent.DriveUploaded)
                }
                .onFailure { fail(it.readableMessage()) }

            DriveAction.RESTORE -> runCatching { driveSync.restore(access.token) }
                .onSuccess {
                    eventChannel.send(BackupEvent.DriveRestored(it.venueCount, it.itemCount))
                }
                .onFailure { fail(it.readableMessage()) }
        }
    }

    private suspend fun fail(reason: String) {
        settingsStore.setSyncFailed(reason)
        eventChannel.send(BackupEvent.DriveFailed(reason))
    }

    private fun Throwable.readableMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.simpleName.orEmpty()

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsStore = appContainer.settingsStore,
                    backupManager = appContainer.backupManager,
                    driveAuth = appContainer.driveAuth,
                    driveSync = appContainer.driveSync,
                )
            }
        }
    }
}
