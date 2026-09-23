package kz.chaykin.zakazano.data.repo

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kz.chaykin.zakazano.data.db.DEFAULT_BIOME_NAME
import kz.chaykin.zakazano.data.db.ZakazanoDatabase
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.data.photo.PhotoCleaner
import kz.chaykin.zakazano.data.photo.PhotoStore
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Venue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Правила биомов: какой открывается, что можно удалить, куда попадает новое заведение.
 * Всё это живёт в репозиториях, а не в DAO, поэтому проверяется здесь.
 */
@RunWith(AndroidJUnit4::class)
class BiomeRepositoryTest {

    private lateinit var database: ZakazanoDatabase
    private lateinit var photosRoot: File
    private val selected = MutableStateFlow(0L)
    private lateinit var biomes: BiomeRepository
    private lateinit var venues: VenueRepository

    // JUnit4 требует от @Before и @After возврата void, поэтому runBlocking, а не runTest.
    @Before
    fun setUp() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ZakazanoDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Уборщик фотографий удаляет всё, на что не ссылается база. С настоящей папкой
        // приложения тест стёр бы реальные снимки на телефоне — поэтому своя папка.
        photosRoot = File(context.cacheDir, "biome-test-${System.nanoTime()}").apply { mkdirs() }
        val sandbox = object : ContextWrapper(context) {
            override fun getFilesDir(): File = photosRoot
            override fun getCacheDir(): File = photosRoot
        }
        val cleaner = PhotoCleaner(database.photoDao(), database.venueDao(), PhotoStore(sandbox))

        biomes = BiomeRepository(
            biomeDao = database.biomeDao(),
            selectedId = selected,
            saveSelected = { selected.value = it },
            photoCleaner = cleaner,
        )
        venues = VenueRepository(
            venueDao = database.venueDao(),
            photoStore = PhotoStore(sandbox),
            photoCleaner = cleaner,
            currentBiomeId = biomes.currentId,
        )
        biomes.ensureDefault()
    }

    @After
    fun tearDown() {
        database.close()
        photosRoot.deleteRecursively()
    }

    @Test
    fun свежая_установка_получает_стандартный_биом_ровно_один_раз() = runTest {
        biomes.ensureDefault()
        biomes.ensureDefault()

        val all = biomes.biomes.first()
        assertEquals(listOf(DEFAULT_BIOME_NAME), all.map { it.name })
        assertEquals(all.single().id, biomes.current.first().id)
    }

    @Test
    fun новый_биом_сразу_становится_текущим() = runTest {
        val thailand = biomes.create("  Тайланд  ")

        assertEquals(thailand, biomes.current.first().id)
        assertEquals("Тайланд", biomes.current.first().name)
    }

    @Test
    fun открывается_последний_выбранный_а_если_его_нет_первый() = runTest {
        val standard = biomes.current.first().id
        val thailand = biomes.create("Тайланд")
        biomes.select(standard)
        assertEquals(standard, biomes.current.first().id)

        biomes.select(thailand)
        assertEquals(thailand, biomes.current.first().id)

        // Выбранный биом удалили (или его заменила копия с Диска) — не повисаем в пустоте.
        biomes.delete(thailand)
        assertEquals(standard, biomes.current.first().id)

        selected.value = 9_999
        assertEquals(standard, biomes.current.first().id)
    }

    @Test
    fun последний_биом_не_удаляется() = runTest {
        val only = biomes.current.first().id

        assertFalse(biomes.delete(only))
        assertEquals(1, biomes.biomes.first().size)
    }

    @Test
    fun новое_заведение_попадает_в_текущий_биом_а_старое_остаётся_в_своём() = runTest {
        val standard = biomes.current.first().id
        val homeVenue = venues.save(Venue(name = "Пельменная"))

        val thailand = biomes.create("Тайланд")
        val tripVenue = venues.save(Venue(name = "Som Tam Nua"))

        // Правим домашнее заведение, пока открыт Тайланд. biomeId = 0 — ровно то, что
        // прислал бы редактор, не успевший загрузить заведение.
        venues.save(Venue(id = homeVenue, name = "Пельменная №5", biomeId = 0))

        val stored = database.venueDao().getAll().associateBy({ it.name }, { it.biomeId })
        assertEquals(standard, stored["Пельменная №5"])
        assertEquals(thailand, stored["Som Tam Nua"])
        assertTrue(tripVenue != homeVenue)
    }

    @Test
    fun перенос_в_другой_биом_забирает_заведение_вместе_с_позициями() = runTest {
        val standard = biomes.current.first().id
        val venueId = venues.save(Venue(name = "Кафе у дома"))
        database.itemDao().insert(
            ItemEntity(
                venueId = venueId,
                name = "Борщ",
                kind = ItemKind.DISH,
                drinkType = null,
                ratingCode = 2,
                priceMinor = null,
                comment = null,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        val thailand = biomes.create("Тайланд")

        venues.moveToBiome(venueId, thailand)

        assertTrue(venues.observeSummaries(standard).first().isEmpty())
        val moved = venues.observeSummaries(thailand).first().single()
        assertEquals("Кафе у дома", moved.venue.name)
        assertEquals(1, moved.dishCount)
    }

    @Test
    fun список_показывает_только_заведения_выбранного_биома() = runTest {
        val standard = biomes.current.first().id
        venues.save(Venue(name = "Пельменная"))
        val thailand = biomes.create("Тайланд")
        venues.save(Venue(name = "Som Tam Nua"))

        assertEquals(listOf("Пельменная"), venues.observeSummaries(standard).first().map { it.venue.name })
        assertEquals(listOf("Som Tam Nua"), venues.observeSummaries(thailand).first().map { it.venue.name })
    }
}
