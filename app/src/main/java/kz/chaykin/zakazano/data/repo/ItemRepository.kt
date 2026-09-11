package kz.chaykin.zakazano.data.repo

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.chaykin.zakazano.data.db.ZakazanoDatabase
import kz.chaykin.zakazano.data.db.dao.ItemDao
import kz.chaykin.zakazano.data.db.dao.PhotoDao
import kz.chaykin.zakazano.data.db.entity.PhotoEntity
import kz.chaykin.zakazano.data.photo.PhotoCleaner
import kz.chaykin.zakazano.data.photo.PhotoStore
import kz.chaykin.zakazano.model.Item
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Photo
import java.io.File

class ItemRepository(
    private val database: ZakazanoDatabase,
    private val itemDao: ItemDao,
    private val photoDao: PhotoDao,
    private val photoStore: PhotoStore,
    private val photoCleaner: PhotoCleaner,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeByVenue(venueId: Long, kind: ItemKind): Flow<List<Item>> =
        itemDao.observeByVenue(venueId, kind).map { rows -> rows.map { it.toDomain() } }

    fun observe(id: Long): Flow<Item?> =
        itemDao.observe(id).map { it?.toDomain() }

    /**
     * Сохраняет позицию вместе со списком фотографий: чего нет в [Item.photos] — отвязывается,
     * что появилось — привязывается. Файлы в редакторе импортируются сразу, поэтому
     * брошенные без сохранения снимки подбирает [PhotoCleaner].
     */
    suspend fun save(item: Item): Long {
        val id = database.withTransaction {
            val timestamp = now()
            val itemId = if (item.id == 0L) {
                itemDao.insert(item.toEntity(createdAt = timestamp, updatedAt = timestamp))
            } else {
                itemDao.update(item.toEntity(createdAt = item.createdAt, updatedAt = timestamp))
                item.id
            }

            val existing = photoDao.byItem(itemId)
            val keptNames = item.photos.map { it.fileName }.toSet()
            existing.filterNot { it.fileName in keptNames }.forEach { photoDao.delete(it.id) }

            val existingNames = existing.map { it.fileName }.toSet()
            item.photos.forEachIndexed { index, photo ->
                if (photo.fileName !in existingNames) {
                    photoDao.insert(
                        PhotoEntity(
                            itemId = itemId,
                            fileName = photo.fileName,
                            sortOrder = index,
                            createdAt = timestamp,
                        ),
                    )
                }
            }
            itemId
        }
        photoCleaner.removeOrphans()
        return id
    }

    suspend fun delete(id: Long) {
        itemDao.delete(id)
        photoCleaner.removeOrphans()
    }

    /** Файл во временной папке, куда системная камера положит снимок. */
    fun newCameraTarget(): File = photoStore.newCameraTempFile()

    /** Импортирует изображение из галереи и отдаёт запись, которую редактор добавит в список. */
    suspend fun importPhoto(uri: Uri): Photo = Photo(fileName = photoStore.importFromUri(uri))

    /** Импортирует снимок, сделанный системной камерой во временный файл. */
    suspend fun importPhoto(file: File): Photo = Photo(fileName = photoStore.importFromFile(file))

    suspend fun discardUnsavedPhotos() = photoCleaner.removeOrphans()
}
