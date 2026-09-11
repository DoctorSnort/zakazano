package kz.chaykin.zakazano.util

import kz.chaykin.zakazano.model.Item
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.ItemSort
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.model.Venue
import kz.chaykin.zakazano.model.VenueSort
import kz.chaykin.zakazano.model.VenueSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderingTest {

    private fun item(
        name: String,
        rating: Rating = Rating.MEH,
        priceMinor: Long? = null,
    ) = Item(venueId = 1, name = name, kind = ItemKind.DISH, rating = rating, priceMinor = priceMinor)

    private fun summary(
        name: String,
        ownRating: Rating? = null,
        average: Double? = null,
        updatedAt: Long = 0,
    ) = VenueSummary(
        venue = Venue(name = name, rating = ownRating, updatedAt = updatedAt),
        dishCount = 0,
        drinkCount = 0,
        averageRating = average,
    )

    @Test
    fun `русский алфавит не путает регистр`() {
        val names = listOf(item("борщ"), item("Азу"), item("Вафли"), item("блины"))
            .orderBy(ItemSort.NAME)
            .map { it.name }

        assertEquals(listOf("Азу", "блины", "борщ", "Вафли"), names)
    }

    @Test
    fun `буква Ё идёт как Е, а не в конце алфавита`() {
        val names = listOf(item("Язык"), item("Ёжик"), item("Ерш"), item("Ананас"))
            .orderBy(ItemSort.NAME)
            .map { it.name }

        // Словарное правило: Ё и Е на первом уровне равны, порядок решают следующие буквы
        // (ж < р), поэтому «Ёжик» встаёт перед «Ерш». В SQL с COLLATE NOCASE обе уехали бы в конец.
        assertEquals(listOf("Ананас", "Ёжик", "Ерш", "Язык"), names)
    }

    @Test
    fun `латиница и кириллица сортируются вместе без падений`() {
        val names = listOf(item("Cola"), item("Борщ"), item("Americano"), item("Айран"))
            .orderBy(ItemSort.NAME)
            .map { it.name }

        assertEquals(4, names.size)
        assertEquals("Americano", names.first())
    }

    @Test
    fun `по оценке сверху оказывается кайф`() {
        val names = listOf(
            item("а", Rating.TERRIBLE),
            item("б", Rating.GREAT),
            item("в", Rating.MEH),
            item("г", Rating.GOOD),
        ).orderBy(ItemSort.RATING).map { it.name }

        assertEquals(listOf("б", "г", "в", "а"), names)
    }

    @Test
    fun `при равной оценке порядок алфавитный`() {
        val names = listOf(
            item("Яблоко", Rating.GREAT),
            item("Ананас", Rating.GREAT),
        ).orderBy(ItemSort.RATING).map { it.name }

        assertEquals(listOf("Ананас", "Яблоко"), names)
    }

    @Test
    fun `позиции без цены уходят в конец списка по цене`() {
        val names = listOf(
            item("без цены", priceMinor = null),
            item("дорогое", priceMinor = 500_000),
            item("дешёвое", priceMinor = 100_000),
        ).orderBy(ItemSort.PRICE).map { it.name }

        assertEquals(listOf("дешёвое", "дорогое", "без цены"), names)
    }

    @Test
    fun `пустой фильтр оценок показывает всё`() {
        val items = listOf(item("а", Rating.TERRIBLE), item("б", Rating.GREAT))
        assertEquals(2, items.filterByRatings(emptySet()).size)
    }

    @Test
    fun `фильтр оставляет только выбранные оценки`() {
        val items = listOf(
            item("а", Rating.TERRIBLE),
            item("б", Rating.GREAT),
            item("в", Rating.GOOD),
        )
        val names = items.filterByRatings(setOf(Rating.GREAT, Rating.GOOD)).map { it.name }

        assertEquals(listOf("б", "в"), names)
    }

    @Test
    fun `своя оценка заведения важнее средней по блюдам`() {
        val summaries = listOf(
            summary("Своя двойка", ownRating = Rating.TERRIBLE, average = 3.0),
            summary("Средняя тройка", average = 2.0),
        ).orderBy(VenueSort.RATING).map { it.venue.name }

        assertEquals(listOf("Средняя тройка", "Своя двойка"), summaries)
    }

    @Test
    fun `заведения без единой оценки уходят вниз`() {
        val names = listOf(
            summary("Пустое"),
            summary("Оценённое", average = 1.0),
        ).orderBy(VenueSort.RATING).map { it.venue.name }

        assertEquals(listOf("Оценённое", "Пустое"), names)
    }

    @Test
    fun `недавние сверху при сортировке по свежести`() {
        val names = listOf(
            summary("старое", updatedAt = 100),
            summary("свежее", updatedAt = 900),
        ).orderBy(VenueSort.RECENT).map { it.venue.name }

        assertEquals(listOf("свежее", "старое"), names)
    }

    @Test
    fun `поиск ищет и по названию, и по адресу`() {
        val summaries = listOf(
            summary("Барбекю"),
            VenueSummary(Venue(name = "Кафе", address = "улица Ленина"), 0, 0, null),
        )

        assertEquals(1, summaries.filterByQuery("барб").size)
        assertEquals(1, summaries.filterByQuery("ленина").size)
        assertEquals(2, summaries.filterByQuery("  ").size)
    }
}
