package kz.chaykin.zakazano.model

data class Photo(
    val id: Long = 0,
    val fileName: String,
    val sortOrder: Int = 0,
)

data class Item(
    val id: Long = 0,
    val venueId: Long,
    val name: String,
    val kind: ItemKind,
    val rating: Rating,
    /** Цена в мелких единицах (тиын/копейки), чтобы не хранить деньги в Double. */
    val priceMinor: Long? = null,
    val comment: String? = null,
    val photos: List<Photo> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
