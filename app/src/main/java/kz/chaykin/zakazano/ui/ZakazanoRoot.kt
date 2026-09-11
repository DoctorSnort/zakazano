package kz.chaykin.zakazano.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.chaykin.zakazano.data.prefs.Settings
import kz.chaykin.zakazano.data.prefs.SettingsStore
import kz.chaykin.zakazano.data.prefs.ThemeMode
import kz.chaykin.zakazano.ui.navigation.ZakazanoNavHost
import kz.chaykin.zakazano.ui.theme.ZakazanoTheme

@Composable
fun ZakazanoRoot(settingsStore: SettingsStore) {
    val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = Settings())

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    ZakazanoTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        ZakazanoNavHost()
    }
}
