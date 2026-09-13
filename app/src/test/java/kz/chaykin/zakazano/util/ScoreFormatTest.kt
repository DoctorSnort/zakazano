package kz.chaykin.zakazano.util

import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.model.Venue
import kz.chaykin.zakazano.model.VenueSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreFormatTest {

    private fun summary(average: Double?) = VenueSummary(
        venue = Venue(name = "Кафе"),
        dishCount = 0,
        drinkCount = 0,
        averageRating = average,
    )

    @Test
    fun `баллы идут от двойки до пятёрки`() {
        assertEquals(2, Rating.TERRIBLE.points)
        assertEquals(3, Rating.MEH.points)
        assertEquals(4, Rating.GOOD.points)
        assertEquals(5, Rating.GREAT.points)
    }

    @Test
    fun `средний балл сдвинут относительно внутренних кодов`() {
        // «кайф» и «неплохо» — это коды 3 и 2, среднее 2.5, в баллах 4.5
        assertEquals(4.5, requireNotNull(summary(2.5).score), 0.0001)
    }

    @Test
    fun `все кайфы дают ровную пятёрку`() {
        assertEquals(5.0, requireNotNull(summary(3.0).score), 0.0001)
    }

    @Test
    fun `все стрёмы дают двойку`() {
        assertEquals(2.0, requireNotNull(summary(0.0).score), 0.0001)
    }

    @Test
    fun `без единой позиции балла нет`() {
        assertNull(summary(null).score)
    }

    @Test
    fun `всегда два знака после запятой`() {
        assertEquals("4,00", ScoreFormat.format(4.0))
        assertEquals("4,50", ScoreFormat.format(4.5))
        assertEquals("2,00", ScoreFormat.format(2.0))
    }

    @Test
    fun `лишние знаки округляются`() {
        assertEquals("3,33", ScoreFormat.format(10.0 / 3))
        assertEquals("4,67", ScoreFormat.format(14.0 / 3))
    }

    @Test
    fun `разделитель запятая, как принято по-русски`() {
        val text = ScoreFormat.format(4.25)
        assertEquals("4,25", text)
        assertEquals(false, text.contains('.'))
    }

    @Test
    fun `балл считается по блюдам и напиткам вместе`() {
        // три позиции: кайф (3), никак (1), неплохо (2) -> среднее кода 2.0 -> 4 балла
        val average = listOf(Rating.GREAT, Rating.MEH, Rating.GOOD).map { it.code }.average()
        val summary = VenueSummary(
            venue = Venue(name = "Кафе"),
            dishCount = 2,
            drinkCount = 1,
            averageRating = average,
        )
        assertEquals("4,00", ScoreFormat.format(requireNotNull(summary.score)))
        assertEquals(ItemKind.entries.size, 2)
    }
}
