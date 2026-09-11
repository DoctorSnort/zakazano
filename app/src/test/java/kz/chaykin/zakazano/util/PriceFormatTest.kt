package kz.chaykin.zakazano.util

import kz.chaykin.zakazano.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceFormatTest {

    @Test
    fun `круглая сумма показывается без копеек`() {
        assertEquals("2\u00A0500\u00A0₽", PriceFormat.format(250_000, Currency.RUB))
    }

    @Test
    fun `копейки показываются когда они есть`() {
        assertEquals("2\u00A0500,50\u00A0₽", PriceFormat.format(250_050, Currency.RUB))
    }

    @Test
    fun `разряды разделяются у больших сумм`() {
        assertEquals("1\u00A0234\u00A0567\u00A0₽", PriceFormat.format(123_456_700, Currency.RUB))
    }

    @Test
    fun `маленькие суммы не разделяются`() {
        assertEquals("90\u00A0₽", PriceFormat.format(9_000, Currency.RUB))
    }

    @Test
    fun `валюта берётся из настроек`() {
        assertEquals("500\u00A0₸", PriceFormat.format(50_000, Currency.KZT))
    }

    @Test
    fun `разбирает целое число`() {
        assertEquals(250_000L, PriceFormat.parse("2500"))
    }

    @Test
    fun `разбирает запятую и точку одинаково`() {
        assertEquals(250_050L, PriceFormat.parse("2500,50"))
        assertEquals(250_050L, PriceFormat.parse("2500.50"))
    }

    @Test
    fun `одна цифра после запятой это десятки копеек`() {
        assertEquals(250_050L, PriceFormat.parse("2500,5"))
    }

    @Test
    fun `пробелы внутри суммы не мешают`() {
        assertEquals(250_000L, PriceFormat.parse("2 500"))
    }

    @Test
    fun `пустая строка это отсутствие цены`() {
        assertNull(PriceFormat.parse(""))
        assertNull(PriceFormat.parse("   "))
    }

    @Test
    fun `мусор не превращается в ноль`() {
        assertNull(PriceFormat.parse("дорого"))
        assertNull(PriceFormat.parse("25,00,5"))
    }

    @Test
    fun `формат и разбор согласованы между собой`() {
        listOf(0L, 1L, 99L, 100L, 250_000L, 123_456_789L).forEach { minor ->
            val text = PriceFormat.format(minor, Currency.RUB).removeSuffix("\u00A0₽")
            assertEquals("не сошлось на $minor", minor, PriceFormat.parse(text))
        }
    }
}
