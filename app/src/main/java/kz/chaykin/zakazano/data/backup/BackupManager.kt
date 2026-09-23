package kz.chaykin.zakazano.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kz.chaykin.zakazano.data.db.DEFAULT_BIOME_NAME
import kz.chaykin.zakazano.data.db.ZakazanoDatabase
import kz.chaykin.zakazano.data.db.dao.ItemWithPhotos
import kz.chaykin.zakazano.data.db.entity.BiomeEntity
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.data.db.entity.PhotoEntity
import kz.chaykin.zakazano.data.db.entity.VenueEntity
import kz.chaykin.zakazano.data.photo.PhotoStore
import kz.chaykin.zakazano.model.DrinkType
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Rating
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Резервная копия — обычный ZIP: `data.json` со всеми записями и папка `photos/` с файлами.
 * Обычный, потому что данные должны быть доставаемы и без этого приложения.
 */
class BackupManager(
    private val context: Context,
    private val database: ZakazanoDatabase,
    private val photoStore: PhotoStore,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(target: Uri): Unit = withContext(Dispatchers.IO) {
        val output = requireNotNull(context.contentResolver.openOutputStream(target)) {
            "Не удалось открыть файл для записи"
        }
        output.use { writeArchive(it) }
    }

    /** Тот же архив, но в обычный файл: так его забирает выгрузка на Google Диск. */
    suspend fun exportTo(target: File): Unit = withContext(Dispatchers.IO) {
        target.outputStream().use { writeArchive(it) }
    }

    private suspend fun writeArchive(output: OutputStream) {
        val biomes = database.biomeDao().getAll()
        val venuesByBiome = database.venueDao().getAll().groupBy { it.biomeId }
        val items = database.itemDao().getAll()
        val itemsByVenue = items.groupBy { it.item.venueId }

        val backup = BackupFile(
            exportedAt = System.currentTimeMillis(),
            biomes = biomes.map { biome ->
                BackupBiome(
                    name = biome.name,
                    createdAt = biome.createdAt,
                    venues = venuesByBiome[biome.id].orEmpty().map { it.toBackup(itemsByVenue[it.id].orEmpty()) },
                )
            },
        )

        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupFile.DATA_ENTRY))
            zip.write(json.encodeToString(backup).toByteArray())
            zip.closeEntry()

            val allVenues = backup.biomes.flatMap { it.venues }
            val referenced = allVenues.flatMap { it.items }.flatMap { it.photos }.toSet() +
                allVenues.mapNotNull { it.photo }.toSet()
            referenced.forEach { fileName ->
                val file = photoStore.file(fileName)
                if (!file.exists()) return@forEach
                zip.putNextEntry(ZipEntry(BackupFile.PHOTOS_PREFIX + fileName))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun VenueEntity.toBackup(items: List<ItemWithPhotos>): BackupVenue = BackupVenue(
        name = name,
        address = address,
        note = note,
        rating = Rating.fromCodeOrNull(ratingCode)?.name,
        photo = photoFileName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        items = items.map { row ->
            BackupItem(
                name = row.item.name,
                kind = row.item.kind.name,
                drinkType = row.item.drinkType?.name,
                rating = Rating.fromCode(row.item.ratingCode).name,
                priceMinor = row.item.priceMinor,
                comment = row.item.comment,
                createdAt = row.item.createdAt,
                updatedAt = row.item.updatedAt,
                photos = row.photos.sortedBy { it.sortOrder }.map { it.fileName },
            )
        },
    )

    /**
     * Полностью заменяет содержимое приложения. Сначала архив копируется во временный файл:
     * ZIP нужно читать вразнобой, а поток из системного выбора файлов этого не умеет.
     */
    suspend fun import(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("import", ".zip", context.cacheDir)
        try {
            requireNotNull(context.contentResolver.openInputStream(source)) {
                "Не удалось открыть файл"
            }.use { input -> temp.outputStream().use { input.copyTo(it) } }
            readArchive(temp)
        } finally {
            temp.delete()
        }
    }

    /** Восстановление из уже скачанного файла — например, из копии на Google Диске. */
    suspend fun importFrom(archive: File): ImportResult = withContext(Dispatchers.IO) {
        readArchive(archive)
    }

    private suspend fun readArchive(temp: File): ImportResult {
        return ZipFile(temp).use { zip ->
            val dataEntry = requireNotNull(zip.getEntry(BackupFile.DATA_ENTRY)) {
                "В файле нет ${BackupFile.DATA_ENTRY} — это не копия «Заказано»"
            }
            val backup = zip.getInputStream(dataEntry).use {
                json.decodeFromString<BackupFile>(it.readBytes().decodeToString())
            }
            require(backup.schemaVersion <= BackupFile.CURRENT_SCHEMA_VERSION) {
                "Копия сделана более новой версией приложения"
            }

            val biomes = backup.biomes.ifEmpty {
                // Копия времён до биомов: всё, что в ней есть, — это «Стандартный».
                listOf(BackupBiome(name = DEFAULT_BIOME_NAME, venues = backup.venues))
            }

            database.withTransaction {
                // Удаление биомов каскадом уносит заведения, позиции и строки фотографий.
                database.biomeDao().deleteAll()
                writeBiomes(biomes)
            }

            photoStore.deleteAll()
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith(BackupFile.PHOTOS_PREFIX) }
                .forEach { entry ->
                    val fileName = entry.name.removePrefix(BackupFile.PHOTOS_PREFIX)
                    // Имена из архива не должны уводить запись за пределы папки с фото.
                    if (fileName.isEmpty() || fileName.contains('/') || fileName.contains('\\')) {
                        return@forEach
                    }
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    photoStore.writeRaw(fileName, bytes)
                }

            ImportResult(
                venueCount = biomes.sumOf { it.venues.size },
                itemCount = biomes.sumOf { biome -> biome.venues.sumOf { it.items.size } },
            )
        }
    }

    private suspend fun writeBiomes(biomes: List<BackupBiome>) {
        biomes.forEach { biome ->
            val biomeId = database.biomeDao().insert(
                BiomeEntity(name = biome.name, createdAt = biome.createdAt),
            )
            writeVenues(biomeId, biome.venues)
        }
    }

    private suspend fun writeVenues(biomeId: Long, venues: List<BackupVenue>) {
        val venueDao = database.venueDao()
        val itemDao = database.itemDao()
        val photoDao = database.photoDao()

        venues.forEach { venue ->
            val venueId = venueDao.insert(
                VenueEntity(
                    biomeId = biomeId,
                    name = venue.name,
                    address = venue.address,
                    note = venue.note,
                    ratingCode = venue.rating?.let { runCatching { Rating.valueOf(it) }.getOrNull()?.code },
                    photoFileName = venue.photo,
                    createdAt = venue.createdAt,
                    updatedAt = venue.updatedAt,
                ),
            )

            venue.items.forEach { item ->
                val itemId = itemDao.insert(
                    ItemEntity(
                        venueId = venueId,
                        name = item.name,
                        kind = runCatching { ItemKind.valueOf(item.kind) }.getOrDefault(ItemKind.DISH),
                        drinkType = item.drinkType?.let { runCatching { DrinkType.valueOf(it) }.getOrNull() },
                        ratingCode = runCatching { Rating.valueOf(item.rating) }
                            .getOrDefault(Rating.MEH).code,
                        priceMinor = item.priceMinor,
                        comment = item.comment,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt,
                    ),
                )

                item.photos.forEachIndexed { index, fileName ->
                    photoDao.insert(
                        PhotoEntity(
                            itemId = itemId,
                            fileName = fileName,
                            sortOrder = index,
                            createdAt = item.createdAt,
                        ),
                    )
                }
            }
        }
    }
}
