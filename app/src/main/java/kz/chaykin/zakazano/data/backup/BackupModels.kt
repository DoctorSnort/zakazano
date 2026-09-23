package kz.chaykin.zakazano.data.backup

import kotlinx.serialization.Serializable

/**
 * Формат резервной копии. Оценки и типы пишутся именами, а не числами: копия должна
 * оставаться читаемой человеком, который откроет data.json через полгода без приложения.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    /** Биомы с их заведениями. Появились в третьей версии копии. */
    val biomes: List<BackupBiome> = emptyList(),
    /**
     * Заведения без биома — так писали копии первой и второй версии. Новые копии
     * оставляют это поле пустым, а при чтении старых всё отсюда уходит в «Стандартный».
     */
    val venues: List<BackupVenue> = emptyList(),
) {
    companion object {
        // 2 — добавились вид напитка и фотография заведения.
        // 3 — заведения разложены по биомам. Все поля необязательные,
        // поэтому копии прошлых версий читаются как есть.
        const val CURRENT_SCHEMA_VERSION = 3
        const val DATA_ENTRY = "data.json"
        const val PHOTOS_PREFIX = "photos/"
    }
}

@Serializable
data class BackupBiome(
    val name: String,
    val createdAt: Long = 0,
    val venues: List<BackupVenue> = emptyList(),
)

@Serializable
data class BackupVenue(
    val name: String,
    val address: String? = null,
    val note: String? = null,
    val rating: String? = null,
    val photo: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val items: List<BackupItem> = emptyList(),
)

@Serializable
data class BackupItem(
    val name: String,
    val kind: String,
    val drinkType: String? = null,
    val rating: String,
    val priceMinor: Long? = null,
    val comment: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val photos: List<String> = emptyList(),
)

data class ImportResult(val venueCount: Int, val itemCount: Int)
