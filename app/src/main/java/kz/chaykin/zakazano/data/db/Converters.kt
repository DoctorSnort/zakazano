package kz.chaykin.zakazano.data.db

import androidx.room.TypeConverter
import kz.chaykin.zakazano.model.ItemKind

class Converters {
    @TypeConverter
    fun fromItemKind(kind: ItemKind): String = kind.name

    @TypeConverter
    fun toItemKind(value: String): ItemKind = ItemKind.valueOf(value)
}
