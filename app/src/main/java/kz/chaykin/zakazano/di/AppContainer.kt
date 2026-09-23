package kz.chaykin.zakazano.di

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kz.chaykin.zakazano.data.backup.BackupManager
import kz.chaykin.zakazano.data.db.ALL_MIGRATIONS
import kz.chaykin.zakazano.data.db.ZakazanoDatabase
import kz.chaykin.zakazano.data.photo.PhotoCleaner
import kz.chaykin.zakazano.data.photo.PhotoStore
import kz.chaykin.zakazano.data.prefs.SettingsStore
import kz.chaykin.zakazano.data.repo.BiomeRepository
import kz.chaykin.zakazano.data.repo.ItemRepository
import kz.chaykin.zakazano.data.repo.VenueRepository
import kz.chaykin.zakazano.data.sync.DriveApi
import kz.chaykin.zakazano.data.sync.DriveAuth
import kz.chaykin.zakazano.data.sync.DriveSync

/**
 * Зависимости приложения собираются руками. Для проекта такого размера Hilt даёт
 * больше сборочной возни, чем пользы: здесь один экземпляр каждого объекта и всё видно глазами.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Для работы, которая должна пережить любой экран: уборка мусора, восстановление из копии. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: ZakazanoDatabase by lazy {
        Room.databaseBuilder(appContext, ZakazanoDatabase::class.java, ZakazanoDatabase.NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    val photoStore: PhotoStore by lazy { PhotoStore(appContext) }

    private val photoCleaner: PhotoCleaner by lazy { PhotoCleaner(database.photoDao(), database.venueDao(), photoStore) }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    val biomeRepository: BiomeRepository by lazy {
        BiomeRepository(
            biomeDao = database.biomeDao(),
            selectedId = settingsStore.currentBiomeId,
            saveSelected = settingsStore::setCurrentBiomeId,
            photoCleaner = photoCleaner,
        )
    }

    val venueRepository: VenueRepository by lazy {
        VenueRepository(database.venueDao(), photoStore, photoCleaner, biomeRepository.currentId)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(appContext, database, photoStore)
    }

    val driveAuth: DriveAuth by lazy { DriveAuth(appContext) }

    val driveSync: DriveSync by lazy { DriveSync(appContext, backupManager, DriveApi()) }

    val itemRepository: ItemRepository by lazy {
        ItemRepository(
            database = database,
            itemDao = database.itemDao(),
            photoDao = database.photoDao(),
            photoStore = photoStore,
            photoCleaner = photoCleaner,
        )
    }
}
