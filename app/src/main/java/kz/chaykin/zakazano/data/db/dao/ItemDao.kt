package kz.chaykin.zakazano.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.model.ItemKind

@Dao
interface ItemDao {

    /**
     * Один запрос на вкладку. Фильтр по оценкам и сортировка делаются в Kotlin:
     * позиций в заведении десятки, зато фильтр переключается мгновенно и без новой подписки,
     * а алфавитный порядок считает [java.text.Collator] — SQLite кириллицу сортировать не умеет.
     */
    @Transaction
    @Query("SELECT * FROM items WHERE venueId = :venueId AND kind = :kind")
    fun observeByVenue(venueId: Long, kind: ItemKind): Flow<List<ItemWithPhotos>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<ItemWithPhotos?>

    @Transaction
    @Query("SELECT * FROM items ORDER BY id")
    suspend fun getAll(): List<ItemWithPhotos>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)
}
