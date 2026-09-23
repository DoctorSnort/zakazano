package kz.chaykin.zakazano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Биом — отдельный набор заведений: «Стандартный» для дома, «Тайланд» для поездки.
 * Друг друга они не видят, общее у них только приложение.
 */
@Entity(tableName = "biomes")
data class BiomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)
