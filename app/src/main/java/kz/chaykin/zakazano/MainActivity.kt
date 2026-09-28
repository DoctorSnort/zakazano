package kz.chaykin.zakazano

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import kz.chaykin.zakazano.ui.ZakazanoRoot

class MainActivity : ComponentActivity() {

    /**
     * Счётчик просьб «открой настройки» — из уведомления о неудачной выгрузке.
     * Счётчик, а не флаг: два тапа по уведомлению подряд должны сработать оба.
     */
    private val openSettingsRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // После поворота экрана интент тот же, и повторно прыгать в настройки незачем.
        if (savedInstanceState == null) handle(intent)

        val container = (application as ZakazanoApp).container
        setContent {
            ZakazanoRoot(
                settingsStore = container.settingsStore,
                openSettingsRequest = openSettingsRequests.intValue,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        // Из «Недавних» система повторяет исходный интент задачи — со всеми extra.
        // Без этой проверки каждое открытие из «Недавних» вело бы в настройки.
        val fromRecents = intent != null &&
            intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (!fromRecents && intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true) {
            openSettingsRequests.intValue++
        }
    }

    companion object {
        const val EXTRA_OPEN_SETTINGS = "kz.chaykin.zakazano.OPEN_SETTINGS"

        /** Открыть приложение сразу на настройках — туда ведёт уведомление об ошибке выгрузки. */
        fun openSettingsIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_SETTINGS, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
