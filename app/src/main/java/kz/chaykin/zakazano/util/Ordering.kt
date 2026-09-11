package kz.chaykin.zakazano.util

import kz.chaykin.zakazano.model.Item
import kz.chaykin.zakazano.model.ItemSort
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.model.VenueSort
import kz.chaykin.zakazano.model.VenueSummary
import java.text.Collator
import java.util.Locale

/**
 * Алфавитный порядок считает [Collator], а не SQLite: `COLLATE NOCASE` умеет только латиницу,
 * поэтому «Ёлки» и «ёлки» в SQL разъезжаются, а «Ё» уезжает в конец алфавита.
 */
object NameOrder {
    private val collator: Collator = Collator.getInstance(Locale.forLanguageTag("ru")).apply {
        strength = Collator.SECONDARY
    }

    val comparator: Comparator<String> = Comparator { left, right -> collator.compare(left, right) }
}

/** Оценка, которая показывается на карточке заведения: своя, а если её нет — средняя по позициям. */
val VenueSummary.effectiveRating: Double?
    get() = venue.rating?.code?.toDouble() ?: averageRating

fun List<Item>.filterByRatings(ratings: Set<Rating>): List<Item> =
    if (ratings.isEmpty()) this else filter { it.rating in ratings }

fun List<Item>.orderBy(sort: ItemSort): List<Item> {
    val byName = compareBy(NameOrder.comparator) { item: Item -> item.name }
    return when (sort) {
        ItemSort.NAME -> sortedWith(byName)
        // Сначала то, что понравилось: смысл списка — вспомнить, что брать снова.
        ItemSort.RATING -> sortedWith(compareByDescending<Item> { it.rating.code }.then(byName))
        // Без цены — в конец: такие строки ничего не говорят о деньгах.
        ItemSort.PRICE -> sortedWith(
            compareBy<Item> { it.priceMinor == null }
                .thenBy { it.priceMinor ?: Long.MAX_VALUE }
                .then(byName),
        )
    }
}

fun List<VenueSummary>.orderBy(sort: VenueSort): List<VenueSummary> {
    val byName = compareBy(NameOrder.comparator) { summary: VenueSummary -> summary.venue.name }
    return when (sort) {
        VenueSort.NAME -> sortedWith(byName)
        VenueSort.RATING -> sortedWith(
            compareBy<VenueSummary> { it.effectiveRating == null }
                .thenByDescending { it.effectiveRating ?: Double.MIN_VALUE }
                .then(byName),
        )
        VenueSort.RECENT -> sortedWith(compareByDescending<VenueSummary> { it.venue.updatedAt }.then(byName))
    }
}

fun List<VenueSummary>.filterByQuery(query: String): List<VenueSummary> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return this
    return filter { summary ->
        summary.venue.name.contains(trimmed, ignoreCase = true) ||
            summary.venue.address?.contains(trimmed, ignoreCase = true) == true
    }
}
