package kz.chaykin.zakazano

import android.app.Application
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.di.AppContainer

class ZakazanoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Снимки, сделанные в редакторе и брошенные без сохранения, убираются при старте:
        // в момент выхода с экрана делать это уже некому.
        container.applicationScope.launch {
            container.itemRepository.discardUnsavedPhotos()
        }
    }
}
