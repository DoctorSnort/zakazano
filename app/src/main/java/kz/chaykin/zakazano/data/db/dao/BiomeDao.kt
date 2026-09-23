package kz.chaykin.zakazano.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kz.chaykin.zakazano.data.db.entity.BiomeEntity

/** Биом вместе с числом заведений — чтобы при удалении честно сказать, сколько уйдёт. */
data class BiomeRow(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val venueCount: Int,
)

@Dao
interface BiomeDao {

    @Query(
        """
        SELECT b.id, b.name, b.createdAt,
               (SELECT COUNT(*) FROM venues v WHERE v.biomeId = b.id) AS venueCount
        FROM biomes b
        ORDER BY b.id
        """,
    )
    fun observeAll(): Flow<List<BiomeRow>>

    @Query("SELECT * FROM biomes ORDER BY id")
    suspend fun getAll(): List<BiomeEntity>

    @Query("SELECT COUNT(*) FROM biomes")
    suspend fun count(): Int

    @Insert
    suspend fun insert(biome: BiomeEntity): Long

    @Query("UPDATE biomes SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** Заведения уходят вместе с биомом по каскаду, а с ними — позиции и строки фотографий. */
    @Query("DELETE FROM biomes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM biomes")
    suspend fun deleteAll()
}
