package kz.chaykin.zakazano.util

import kz.chaykin.zakazano.model.Currency
import kotlin.math.absoluteValue

/**
 * Деньги хранятся в целых мелких единицах (копейках), а не в Double: 0.1 + 0.2 в Double
 * не равно 0.3, и на сумме заказов это однажды вылезет.
 */
object PriceFormat {

    private const val MINOR_IN_MAJOR = 100L
    private const val THIN_SPACE = '\u00A0'

    /** «2 500 ₽», а копейки показываются только если они есть: «2 500,50 ₽». */
    fun format(priceMinor: Long, currency: Currency): String {
        val major = priceMinor / MINOR_IN_MAJOR
        val minor = (priceMinor % MINOR_IN_MAJOR).absoluteValue
        val groupedMajor = groupThousands(major)
        val amount = if (minor == 0L) groupedMajor else "$groupedMajor,${minor.toString().padStart(2, '0')}"
        return "$amount$THIN_SPACE${currency.symbol}"
    }

    /**
     * То же число для поля ввода: без разрядов и без валюты, чтобы редактировать было удобно
     * и чтобы [parse] принял результат обратно без потерь.
     */
    fun toInput(priceMinor: Long): String {
        val major = priceMinor / MINOR_IN_MAJOR
        val minor = (priceMinor % MINOR_IN_MAJOR).absoluteValue
        return if (minor == 0L) major.toString() else "$major,${minor.toString().padStart(2, '0')}"
    }

    /**
     * Разбирает то, что человек набрал руками: «2500», «2 500», «2500,50», «2500.5».
     * Возвращает null, если строка не похожа на цену — пустую строку считаем «цена не указана».
     */
    fun parse(input: String): Long? {
        val cleaned = input.filterNot { it.isWhitespace() || it == THIN_SPACE }.replace(',', '.')
        if (cleaned.isEmpty()) return null
        if (!cleaned.all { it.isDigit() || it == '.' }) return null

        val parts = cleaned.split('.')
        if (parts.size > 2) return null

        val major = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val minor = when {
            parts.size == 1 -> 0L
            else -> parts[1].take(2).padEnd(2, '0').toLongOrNull() ?: return null
        }
        return major * MINOR_IN_MAJOR + minor
    }

    private fun groupThousands(value: Long): String {
        val digits = value.absoluteValue.toString()
        val grouped = digits.reversed().chunked(3).joinToString(THIN_SPACE.toString()).reversed()
        return if (value < 0) "-$grouped" else grouped
    }
}
