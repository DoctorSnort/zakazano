package kz.chaykin.zakazano.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kz.chaykin.zakazano.data.db.entity.PhotoEntity

@Dao
interface PhotoDao {

    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos WHERE itemId = :itemId ORDER BY sortOrder")
    suspend fun byItem(itemId: Long): List<PhotoEntity>

    @Query("SELECT fileName FROM photos")
    suspend fun allFileNames(): List<String>

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM photos WHERE itemId = :itemId")
    suspend fun deleteByItem(itemId: Long)
}
