package kz.chaykin.zakazano.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kz.chaykin.zakazano.data.db.dao.BiomeDao
import kz.chaykin.zakazano.data.db.dao.ItemDao
import kz.chaykin.zakazano.data.db.dao.PhotoDao
import kz.chaykin.zakazano.data.db.dao.VenueDao
import kz.chaykin.zakazano.data.db.entity.BiomeEntity
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.data.db.entity.PhotoEntity
import kz.chaykin.zakazano.data.db.entity.VenueEntity

@Database(
    entities = [BiomeEntity::class, VenueEntity::class, ItemEntity::class, PhotoEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ZakazanoDatabase : RoomDatabase() {
    abstract fun biomeDao(): BiomeDao
    abstract fun venueDao(): VenueDao
    abstract fun itemDao(): ItemDao
    abstract fun photoDao(): PhotoDao

    companion object {
        const val NAME = "zakazano.db"
    }
}
