package kz.chaykin.zakazano.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.data.db.entity.PhotoEntity
import kz.chaykin.zakazano.data.db.entity.VenueEntity
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Rating
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZakazanoDatabaseTest {

    private lateinit var database: ZakazanoDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ZakazanoDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertVenue(name: String = "Кафе", ratingCode: Int? = null): Long =
        database.venueDao().insert(
            VenueEntity(
                name = name,
                address = null,
                note = null,
                ratingCode = ratingCode,
                createdAt = 1,
                updatedAt = 1,
            ),
        )

    private suspend fun insertItem(
        venueId: Long,
        name: String,
        kind: ItemKind = ItemKind.DISH,
        rating: Rating = Rating.GOOD,
    ): Long = database.itemDao().insert(
        ItemEntity(
            venueId = venueId,
            name = name,
            kind = kind,
            ratingCode = rating.code,
            priceMinor = null,
            comment = null,
            createdAt = 1,
            updatedAt = 1,
        ),
    )

    @Test
    fun позиции_сохраняются_и_читаются_по_вкладкам() = runTest {
        val venueId = insertVenue()
        insertItem(venueId, "Борщ", ItemKind.DISH)
        insertItem(venueId, "Компот", ItemKind.DRINK)

        val dishes = database.itemDao().observeByVenue(venueId, ItemKind.DISH).first()
        val drinks = database.itemDao().observeByVenue(venueId, ItemKind.DRINK).first()

        assertEquals(listOf("Борщ"), dishes.map { it.item.name })
        assertEquals(listOf("Компот"), drinks.map { it.item.name })
    }

    @Test
    fun удаление_заведения_уносит_позиции_и_строки_фотографий() = runTest {
        val venueId = insertVenue()
        val itemId = insertItem(venueId, "Борщ")
        database.photoDao().insert(
            PhotoEntity(itemId = itemId, fileName = "a.jpg", sortOrder = 0, createdAt = 1),
        )

        database.venueDao().delete(venueId)

        assertTrue(database.itemDao().getAll().isEmpty())
        assertTrue(database.photoDao().allFileNames().isEmpty())
    }

    @Test
    fun сводка_считает_блюда_напитки_и_среднюю_оценку() = runTest {
        val venueId = insertVenue()
        insertItem(venueId, "Борщ", ItemKind.DISH, Rating.GREAT)
        insertItem(venueId, "Плов", ItemKind.DISH, Rating.MEH)
        insertItem(venueId, "Морс", ItemKind.DRINK, Rating.GOOD)

        val summary = database.venueDao().observeSummaries().first().single()

        assertEquals(2, summary.dishCount)
        assertEquals(1, summary.drinkCount)
        // (3 + 1 + 2) / 3
        assertEquals(2.0, requireNotNull(summary.averageRating), 0.0001)
    }

    @Test
    fun заведение_без_позиций_не_имеет_средней_оценки() = runTest {
        insertVenue()

        val summary = database.venueDao().observeSummaries().first().single()

        assertEquals(0, summary.dishCount)
        assertEquals(0, summary.drinkCount)
        assertNull(summary.averageRating)
    }

    @Test
    fun фотографии_возвращаются_вместе_с_позицией_в_нужном_порядке() = runTest {
        val venueId = insertVenue()
        val itemId = insertItem(venueId, "Борщ")
        database.photoDao().insert(
            PhotoEntity(itemId = itemId, fileName = "second.jpg", sortOrder = 1, createdAt = 1),
        )
        database.photoDao().insert(
            PhotoEntity(itemId = itemId, fileName = "first.jpg", sortOrder = 0, createdAt = 1),
        )

        val photos = database.photoDao().byItem(itemId).map { it.fileName }

        assertEquals(listOf("first.jpg", "second.jpg"), photos)
    }
}
