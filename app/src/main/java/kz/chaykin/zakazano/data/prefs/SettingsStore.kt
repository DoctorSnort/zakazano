package kz.chaykin.zakazano.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }
}
