package kz.chaykin.zakazano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String?,
    val note: String?,
    /** Код оценки заведения, null — не оценено. Хранится числом, см. [kz.chaykin.zakazano.model.Rating]. */
    val ratingCode: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)
