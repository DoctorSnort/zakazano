package kz.chaykin.zakazano.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kz.chaykin.zakazano.data.db.entity.VenueEntity

@Dao
interface VenueDao {

    /**
     * Список заведений со счётчиками и средней оценкой — всё одним запросом.
     * Считать среднее в Kotlin означало бы тянуть в память каждую позицию каждого заведения
     * только ради одного числа на карточке.
     */
    @Query(
        """
        SELECT v.*,
               (SELECT COUNT(*) FROM items i WHERE i.venueId = v.id AND i.kind = 'DISH') AS dishCount,
               (SELECT COUNT(*) FROM items i WHERE i.venueId = v.id AND i.kind = 'DRINK') AS drinkCount,
               (SELECT AVG(i.ratingCode) FROM items i WHERE i.venueId = v.id) AS averageRating
        FROM venues v
        WHERE v.biomeId = :biomeId
        """,
    )
    fun observeSummaries(biomeId: Long): Flow<List<VenueSummaryRow>>

    @Query("SELECT * FROM venues WHERE id = :id")
    fun observe(id: Long): Flow<VenueEntity?>

    @Query("SELECT * FROM venues ORDER BY id")
    suspend fun getAll(): List<VenueEntity>

    /** Имена файлов фотографий заведений — нужны уборщику, чтобы он их не снёс. */
    @Query("SELECT photoFileName FROM venues WHERE photoFileName IS NOT NULL")
    suspend fun allPhotoFileNames(): List<String>

    @Insert
    suspend fun insert(venue: VenueEntity): Long

    @Update
    suspend fun update(venue: VenueEntity)

    @Query("DELETE FROM venues WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM venues")
    suspend fun deleteAll()
}
