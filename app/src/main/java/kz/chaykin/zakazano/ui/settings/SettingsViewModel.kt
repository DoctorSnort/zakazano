package kz.chaykin.zakazano.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.data.backup.BackupManager
import kz.chaykin.zakazano.data.prefs.Settings
import kz.chaykin.zakazano.data.prefs.SettingsStore
import kz.chaykin.zakazano.data.prefs.ThemeMode
import kz.chaykin.zakazano.model.Currency
import kz.chaykin.zakazano.ui.appContainer

/** Одноразовые события: их нельзя держать в состоянии, иначе снекбар повторится при повороте. */
sealed interface BackupEvent {
    data object Exported : BackupEvent
    data class ExportFailed(val reason: String) : BackupEvent
    data class Imported(val venues: Int, val items: Int) : BackupEvent
    data class ImportFailed(val reason: String) : BackupEvent
}

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = Settings(),
    )

    private val eventChannel = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = eventChannel.receiveAsFlow()

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

    private fun Throwable.readableMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.simpleName.orEmpty()

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                SettingsViewModel(appContainer.settingsStore, appContainer.backupManager)
            }
        }
    }
}
