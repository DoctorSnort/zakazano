package kz.chaykin.zakazano.model

/** Отдельная вселенная заведений — город, страна, поездка. */
data class Biome(
    val id: Long,
    val name: String,
    val venueCount: Int = 0,
)
