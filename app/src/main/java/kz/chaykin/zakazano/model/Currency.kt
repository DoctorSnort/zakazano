package kz.chaykin.zakazano.model

enum class Currency(val code: String, val symbol: String) {
    RUB("RUB", "₽"),
    KZT("KZT", "₸"),
    USD("USD", "$"),
    EUR("EUR", "€"),
    ;

    companion object {
        val Default = RUB

        fun fromCodeOrDefault(code: String?): Currency =
            entries.firstOrNull { it.code == code } ?: Default
    }
}
