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

    companion object {
        fun fromCode(code: Int): Rating =
            entries.firstOrNull { it.code == code }
                ?: error("Неизвестный код оценки: $code")

        fun fromCodeOrNull(code: Int?): Rating? =
            code?.let { c -> entries.firstOrNull { it.code == c } }
    }
}
