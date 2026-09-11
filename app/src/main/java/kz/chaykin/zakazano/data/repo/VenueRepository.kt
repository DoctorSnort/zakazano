package kz.chaykin.zakazano.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.chaykin.zakazano.data.db.dao.VenueDao
import kz.chaykin.zakazano.data.photo.PhotoCleaner
import kz.chaykin.zakazano.model.Venue
import kz.chaykin.zakazano.model.VenueSummary

class VenueRepository(
    private val venueDao: VenueDao,
    private val photoCleaner: PhotoCleaner,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeSummaries(): Flow<List<VenueSummary>> =
        venueDao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    fun observe(id: Long): Flow<Venue?> =
        venueDao.observe(id).map { it?.toDomain() }

    /** Возвращает id: для нового заведения — только что выданный. */
    suspend fun save(venue: Venue): Long {
        val timestamp = now()
        return if (venue.id == 0L) {
            venueDao.insert(venue.toEntity(createdAt = timestamp, updatedAt = timestamp))
        } else {
            venueDao.update(venue.toEntity(createdAt = venue.createdAt, updatedAt = timestamp))
            venue.id
        }
    }

    suspend fun delete(id: Long) {
        venueDao.delete(id)
        photoCleaner.removeOrphans()
    }
}
