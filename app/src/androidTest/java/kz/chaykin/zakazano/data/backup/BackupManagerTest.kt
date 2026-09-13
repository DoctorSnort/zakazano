package kz.chaykin.zakazano.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kz.chaykin.zakazano.data.db.ZakazanoDatabase
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.data.db.entity.PhotoEntity
import kz.chaykin.zakazano.data.db.entity.VenueEntity
import kz.chaykin.zakazano.data.photo.PhotoStore
import kz.chaykin.zakazano.model.DrinkType
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Rating
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Резервная копия — единственная страховка от потери телефона, поэтому проверяем
 * именно круг: выгрузили, стёрли всё, восстановили, получили то же самое вместе с файлами фото.
 */
@RunWith(AndroidJUnit4::class)
class BackupManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: ZakazanoDatabase
    private lateinit var photoStore: PhotoStore
    private lateinit var backupManager: BackupManager
    private lateinit var archive: File

    private companion object {
        const val VENUE_PHOTO = "venue-test.jpg"
    }

    // JUnit4 требует от @Before и @After возврата void, поэтому здесь runBlocking, а не runTest:
    // с runTest весь класс молча не запускается.
    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, ZakazanoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        photoStore = PhotoStore(context)
        photoStore.deleteAll()
        backupManager = BackupManager(context, database, photoStore)
        archive = File(context.cacheDir, "backup-test.zip")
    }

    @After
    fun tearDown() = runBlocking {
        database.close()
        archive.delete()
        photoStore.deleteAll()
    }

    private suspend fun seed(): String {
        val venueId = database.venueDao().insert(
            VenueEntity(
                name = "Кафе на Ёлочной",
                address = "улица Ёлочная, 1",
                note = "шумно",
                ratingCode = Rating.GOOD.code,
                photoFileName = VENUE_PHOTO,
                createdAt = 100,
                updatedAt = 200,
            ),
        )
        val itemId = database.itemDao().insert(
            ItemEntity(
                venueId = venueId,
                name = "Плов",
                kind = ItemKind.DISH,
                drinkType = null,
                ratingCode = Rating.GREAT.code,
                priceMinor = 250_050,
                comment = "мяса много",
                createdAt = 100,
                updatedAt = 200,
            ),
        )
        database.itemDao().insert(
            ItemEntity(
                venueId = venueId,
                name = "Негрони",
                kind = ItemKind.DRINK,
                drinkType = DrinkType.COCKTAIL,
                ratingCode = Rating.GREAT.code,
                priceMinor = 180_000,
                comment = null,
                createdAt = 100,
                updatedAt = 200,
            ),
        )
        photoStore.writeRaw(VENUE_PHOTO, byteArrayOf(9, 9, 9))
        val fileName = "photo-test.jpg"
        photoStore.writeRaw(fileName, byteArrayOf(1, 2, 3, 4, 5))
        database.photoDao().insert(
            PhotoEntity(itemId = itemId, fileName = fileName, sortOrder = 0, createdAt = 100),
        )
        return fileName
    }

    @Test
    fun копия_переживает_полную_очистку_приложения() = runTest {
        val fileName = seed()

        backupManager.export(Uri.fromFile(archive))
        assertTrue("архив не создан", archive.length() > 0)

        database.venueDao().deleteAll()
        photoStore.deleteAll()
        assertTrue(database.venueDao().getAll().isEmpty())

        val result = backupManager.import(Uri.fromFile(archive))

        assertEquals(1, result.venueCount)
        assertEquals(2, result.itemCount)

        val venue = database.venueDao().getAll().single()
        assertEquals("Кафе на Ёлочной", venue.name)
        assertEquals("улица Ёлочная, 1", venue.address)
        assertEquals("шумно", venue.note)
        assertEquals(Rating.GOOD.code, venue.ratingCode)
        assertEquals("фотография заведения не вернулась", VENUE_PHOTO, venue.photoFileName)
        assertTrue("файл фотографии заведения не восстановлен", photoStore.file(VENUE_PHOTO).exists())

        val drink = database.itemDao().getAll().single { it.item.name == "Негрони" }
        assertEquals(DrinkType.COCKTAIL, drink.item.drinkType)

        val restored = database.itemDao().getAll().single { it.item.name == "Плов" }
        assertEquals("Плов", restored.item.name)
        assertEquals(ItemKind.DISH, restored.item.kind)
        assertEquals(Rating.GREAT.code, restored.item.ratingCode)
        assertEquals(250_050L, restored.item.priceMinor)
        assertEquals("мяса много", restored.item.comment)
        assertEquals(listOf(fileName), restored.photos.map { it.fileName })

        val photoFile = photoStore.file(fileName)
        assertTrue("файл фотографии не восстановлен", photoFile.exists())
        assertEquals(5, photoFile.length())
    }

    @Test
    fun восстановление_замещает_то_что_было_а_не_добавляется_к_нему() = runTest {
        seed()
        backupManager.export(Uri.fromFile(archive))

        database.venueDao().insert(
            VenueEntity(
                name = "Лишнее заведение",
                address = null,
                note = null,
                ratingCode = null,
                photoFileName = null,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        assertEquals(2, database.venueDao().getAll().size)

        backupManager.import(Uri.fromFile(archive))

        assertEquals(listOf("Кафе на Ёлочной"), database.venueDao().getAll().map { it.name })
    }

    @Test
    fun чужой_zip_не_ломает_данные() = runTest {
        seed()
        val foreign = File(context.cacheDir, "foreign.zip")
        java.util.zip.ZipOutputStream(foreign.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("readme.txt"))
            zip.write("не копия".toByteArray())
            zip.closeEntry()
        }

        val failure = runCatching { backupManager.import(Uri.fromFile(foreign)) }

        assertTrue("импорт чужого архива должен падать", failure.isFailure)
        assertEquals(1, database.venueDao().getAll().size)
        foreign.delete()
    }
}
