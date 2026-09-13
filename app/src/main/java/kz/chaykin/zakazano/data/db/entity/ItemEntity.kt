package kz.chaykin.zakazano.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kz.chaykin.zakazano.model.DrinkType
import kz.chaykin.zakazano.model.ItemKind

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = VenueEntity::class,
            parentColumns = ["id"],
            childColumns = ["venueId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("venueId")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venueId: Long,
    val name: String,
    /** Строкой — по нему только фильтруют вкладки, читаемость в БД важнее компактности. */
    val kind: ItemKind,
    /** Вид напитка, строкой и с null для блюд. Появился во второй версии базы. */
    val drinkType: DrinkType?,
    /**
     * Числом, а не строкой: по этой колонке SQL считает AVG для средней оценки заведения.
     * Со строкой пришлось бы вытаскивать все позиции в память ради одного числа.
     */
    val ratingCode: Int,
    val priceMinor: Long?,
    val comment: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
