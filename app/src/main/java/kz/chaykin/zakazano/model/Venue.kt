package kz.chaykin.zakazano.model

data class Venue(
    val id: Long = 0,
    val name: String,
    val address: String? = null,
    val note: String? = null,
    /** Своя оценка месту — атмосфера, сервис. Отдельно от средней по блюдам. */
    val rating: Rating? = null,
    /** Имя файла фотографии заведения в files/photos, null — фотографии нет. */
    val photoFileName: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

/** Заведение вместе с тем, что показывается в списке без захода внутрь. */
data class VenueSummary(
    val venue: Venue,
    val dishCount: Int,
    val drinkCount: Int,
    /** Среднее по кодам оценок всех позиций, null — если ничего ещё не оценено. */
    val averageRating: Double?,
)
