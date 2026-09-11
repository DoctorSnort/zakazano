package kz.chaykin.zakazano.data.db.dao

import androidx.room.Embedded
import androidx.room.Relation
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.data.db.entity.PhotoEntity

data class ItemWithPhotos(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val photos: List<PhotoEntity>,
)
