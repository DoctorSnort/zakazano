package kz.chaykin.zakazano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kz.chaykin.zakazano.ui.ZakazanoRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as ZakazanoApp).container
        setContent {
            ZakazanoRoot(settingsStore = container.settingsStore)
        }
    }
}
