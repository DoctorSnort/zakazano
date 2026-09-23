package kz.chaykin.zakazano.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kz.chaykin.zakazano.data.db.DEFAULT_BIOME_NAME
import kz.chaykin.zakazano.data.db.dao.BiomeDao
import kz.chaykin.zakazano.data.db.entity.BiomeEntity
import kz.chaykin.zakazano.data.photo.PhotoCleaner
import kz.chaykin.zakazano.model.Biome

class BiomeRepository(
    private val biomeDao: BiomeDao,
    /** Id последнего выбранного биома; 0 — ещё не выбирали. */
    selectedId: Flow<Long>,
    private val saveSelected: suspend (Long) -> Unit,
    private val photoCleaner: PhotoCleaner,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val biomes: Flow<List<Biome>> = biomeDao.observeAll().map { rows ->
        rows.map { Biome(id = it.id, name = it.name, venueCount = it.venueCount) }
    }

    /**
     * Текущий биом — последний выбранный. Если его успели удалить (или копия с Диска
     * заменила все биомы), берём первый: открываться в пустоту незачем.
     */
    val current: Flow<Biome> = combine(biomes, selectedId) { all, selected ->
        all.firstOrNull { it.id == selected } ?: all.firstOrNull()
    }.filterNotNull().distinctUntilChanged()

    /** Id текущего биома — нужен заведению, которое создаётся прямо сейчас. */
    val currentId: Flow<Long> = current.map { it.id }.distinctUntilChanged()

    /** Свежая база или копия без биомов: без хотя бы одного приложению не с чего начать. */
    suspend fun ensureDefault() {
        if (biomeDao.count() == 0) {
            biomeDao.insert(BiomeEntity(name = DEFAULT_BIOME_NAME, createdAt = now()))
        }
    }

    suspend fun select(id: Long) = saveSelected(id)

    /** Новый биом сразу становится текущим: его создают, чтобы в него записывать. */
    suspend fun create(name: String): Long {
        val id = biomeDao.insert(BiomeEntity(name = name.trim(), createdAt = now()))
        select(id)
        return id
    }

    suspend fun rename(id: Long, name: String) = biomeDao.rename(id, name.trim())

    /** Последний биом не удаляется — иначе главному экрану нечего показывать. */
    suspend fun delete(id: Long): Boolean {
        if (biomeDao.count() <= 1) return false
        biomeDao.delete(id)
        photoCleaner.removeOrphans()
        return true
    }
}
