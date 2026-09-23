package kz.chaykin.zakazano.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.chaykin.zakazano.model.Currency

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You — подхватывать цвета обоев вместо собственной палитры. */
    val dynamicColor: Boolean = false,
    val currency: Currency = Currency.Default,
)

/**
 * Всё, что приложение помнит про Google Диск. Токен доступа здесь не хранится:
 * он живёт около часа и запрашивается заново перед каждой выгрузкой.
 */
data class SyncState(
    val connected: Boolean = false,
    val accountEmail: String? = null,
    /** Автоматическая выгрузка раз в сутки. */
    val autoDaily: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastError: String? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[KeyThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[KeyDynamicColor] ?: false,
            currency = Currency.fromCodeOrDefault(prefs[KeyCurrency]),
        )
    }

    val sync: Flow<SyncState> = context.dataStore.data.map { prefs ->
        SyncState(
            connected = prefs[KeyDriveConnected] ?: false,
            accountEmail = prefs[KeyDriveEmail],
            autoDaily = prefs[KeyDriveAuto] ?: false,
            lastSyncAt = prefs[KeyDriveLastSync] ?: 0L,
            lastError = prefs[KeyDriveLastError],
        )
    }

    /** Последний выбранный биом, 0 — ещё не выбирали. */
    val currentBiomeId: Flow<Long> = context.dataStore.data.map { it[KeyCurrentBiome] ?: 0L }

    suspend fun setCurrentBiomeId(id: Long) {
        context.dataStore.edit { it[KeyCurrentBiome] = id }
    }

    suspend fun setDriveConnected(email: String?) {
        context.dataStore.edit { prefs ->
            prefs[KeyDriveConnected] = true
            if (email != null) prefs[KeyDriveEmail] = email
            prefs.remove(KeyDriveLastError)
        }
    }

    suspend fun clearDrive() {
        context.dataStore.edit { prefs ->
            prefs.remove(KeyDriveConnected)
            prefs.remove(KeyDriveEmail)
            prefs.remove(KeyDriveAuto)
            prefs.remove(KeyDriveLastSync)
            prefs.remove(KeyDriveLastError)
        }
    }

    suspend fun setAutoDaily(enabled: Boolean) {
        context.dataStore.edit { it[KeyDriveAuto] = enabled }
    }

    suspend fun setSyncSucceeded(at: Long) {
        context.dataStore.edit { prefs ->
            prefs[KeyDriveLastSync] = at
            prefs.remove(KeyDriveLastError)
        }
    }

    suspend fun setSyncFailed(reason: String) {
        context.dataStore.edit { it[KeyDriveLastError] = reason }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KeyThemeMode] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KeyDynamicColor] = enabled }
    }

    suspend fun setCurrency(currency: Currency) {
        context.dataStore.edit { it[KeyCurrency] = currency.code }
    }

    private companion object {
        val KeyThemeMode = stringPreferencesKey("theme_mode")
        val KeyDynamicColor = booleanPreferencesKey("dynamic_color")
        val KeyCurrency = stringPreferencesKey("currency")
        val KeyCurrentBiome = longPreferencesKey("current_biome_id")
        val KeyDriveConnected = booleanPreferencesKey("drive_connected")
        val KeyDriveEmail = stringPreferencesKey("drive_email")
        val KeyDriveAuto = booleanPreferencesKey("drive_auto_daily")
        val KeyDriveLastSync = longPreferencesKey("drive_last_sync")
        val KeyDriveLastError = stringPreferencesKey("drive_last_error")
    }
}
