package kz.chaykin.zakazano.util

import java.util.Locale

/** Средний балл заведения для показа: два знака после запятой, по-русски — через запятую. */
object ScoreFormat {

    private val locale: Locale = Locale.forLanguageTag("ru")

    fun format(score: Double): String = String.format(locale, "%.2f", score)
}
