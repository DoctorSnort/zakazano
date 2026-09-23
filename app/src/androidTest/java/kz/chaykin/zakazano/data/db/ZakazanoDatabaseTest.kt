package kz.chaykin.zakazano.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kz.chaykin.zakazano.data.db.entity.BiomeEntity
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
    private var biomeId: Long = 0

    // JUnit4 требует от @Before возврата void, поэтому runBlocking, а не runTest.
    @Before
    fun setUp() = kotlinx.coroutines.runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ZakazanoDatabase::class.java,
        ).build()
        biomeId = insertBiome("Стандартный")
    }

    private suspend fun insertBiome(name: String): Long =
        database.biomeDao().insert(BiomeEntity(name = name, createdAt = 1))

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertVenue(
        name: String = "Кафе",
        ratingCode: Int? = null,
        biome: Long = biomeId,
    ): Long =
        database.venueDao().insert(
            VenueEntity(
                biomeId = biome,
                name = name,
                address = null,
                note = null,
                ratingCode = ratingCode,
                photoFileName = null,
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
            drinkType = null,
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

        val summary = database.venueDao().observeSummaries(biomeId).first().single()

        assertEquals(2, summary.dishCount)
        assertEquals(1, summary.drinkCount)
        // (3 + 1 + 2) / 3
        assertEquals(2.0, requireNotNull(summary.averageRating), 0.0001)
    }

    @Test
    fun заведение_без_позиций_не_имеет_средней_оценки() = runTest {
        insertVenue()

        val summary = database.venueDao().observeSummaries(biomeId).first().single()

        assertEquals(0, summary.dishCount)
        assertEquals(0, summary.drinkCount)
        assertNull(summary.averageRating)
    }

    @Test
    fun биомы_не_видят_заведений_друг_друга() = runTest {
        val thailand = insertBiome("Тайланд")
        insertVenue("Пельменная")
        insertVenue("Som Tam Nua", biome = thailand)

        val home = database.venueDao().observeSummaries(biomeId).first().map { it.venue.name }
        val trip = database.venueDao().observeSummaries(thailand).first().map { it.venue.name }

        assertEquals(listOf("Пельменная"), home)
        assertEquals(listOf("Som Tam Nua"), trip)
    }

    @Test
    fun удаление_биома_уносит_его_заведения_и_позиции_но_не_чужие() = runTest {
        val thailand = insertBiome("Тайланд")
        val homeVenue = insertVenue("Пельменная")
        insertItem(homeVenue, "Пельмени")
        val tripVenue = insertVenue("Som Tam Nua", biome = thailand)
        insertItem(tripVenue, "Том ям")

        database.biomeDao().delete(thailand)

        assertEquals(listOf("Пельменная"), database.venueDao().getAll().map { it.name })
        assertEquals(listOf("Пельмени"), database.itemDao().getAll().map { it.item.name })
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
