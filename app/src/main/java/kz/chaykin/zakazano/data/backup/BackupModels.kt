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
    val venues: List<BackupVenue>,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val DATA_ENTRY = "data.json"
        const val PHOTOS_PREFIX = "photos/"
    }
}

@Serializable
data class BackupVenue(
    val name: String,
    val address: String? = null,
    val note: String? = null,
    val rating: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val items: List<BackupItem> = emptyList(),
)

@Serializable
data class BackupItem(
    val name: String,
    val kind: String,
    val rating: String,
    val priceMinor: Long? = null,
    val comment: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val photos: List<String> = emptyList(),
)

data class ImportResult(val venueCount: Int, val itemCount: Int)
