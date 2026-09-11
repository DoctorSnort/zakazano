package kz.chaykin.zakazano.data.photo

import kz.chaykin.zakazano.data.db.dao.PhotoDao

/**
 * Удаляет файлы, на которые больше никто не ссылается.
 *
 * Каскад в базе сносит строки фотографий вместе с позицией, но файлы на диске он не трогает —
 * про них знает только приложение. Сюда же попадают снимки, сделанные в редакторе
 * и брошенные без сохранения.
 */
class PhotoCleaner(
    private val photoDao: PhotoDao,
    private val photoStore: PhotoStore,
) {
    suspend fun removeOrphans() {
        val referenced = photoDao.allFileNames().toSet()
        photoStore.listFileNames()
            .filterNot { it in referenced }
            .forEach { photoStore.delete(it) }
    }
}
