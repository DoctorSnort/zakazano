package kz.chaykin.zakazano.model

/**
 * Оценка блюда. Намеренно четыре ступени и никаких звёздочек: оценивать нужно
 * за секунду, не задумываясь «это 7 или 8 из 10».
 *
 * [code] пишется в базу вместо имени, чтобы среднюю оценку заведения считал SQL,
 * а не Kotlin. Значения зафиксированы навсегда — менять их нельзя, иначе поедут
 * уже сохранённые записи.
 */
enum class Rating(val code: Int) {
    TERRIBLE(0),
    MEH(1),
    GOOD(2),
    GREAT(3),
    ;

    /**
     * Оценка в привычных баллах: стрём = 2, никак = 3, неплохо = 4, кайф = 5.
     * Нужна там, где оценки усредняются: «в среднем 4,25» человек понимает сразу,
     * а «в среднем 2,25» по внутренним кодам — нет.
     */
    val points: Int get() = code + POINTS_OFFSET

    companion object {
        /** Сдвиг между внутренним кодом (0..3) и баллами (2..5). */
        const val POINTS_OFFSET = 2

        fun fromCode(code: Int): Rating =
            entries.firstOrNull { it.code == code }
                ?: error("Неизвестный код оценки: $code")

        fun fromCodeOrNull(code: Int?): Rating? =
            code?.let { c -> entries.firstOrNull { it.code == c } }
    }
}
