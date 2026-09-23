package kz.chaykin.zakazano.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Переезд на вторую версию: вид напитка и фотография заведения.
 *
 * Миграция, а не пересоздание базы: у приложения уже есть пользователь с записями,
 * и терять их при обновлении нельзя. Обе колонки добавляются пустыми —
 * у старых записей вид напитка и фотография просто отсутствуют.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN drinkType TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE venues ADD COLUMN photoFileName TEXT DEFAULT NULL")
    }
}

/**
 * Переезд на третью версию: биомы.
 *
 * Все существующие заведения попадают в биом «Стандартный» с id = 1 — ровно то,
 * что пользователь видел до обновления. Таблицу заведений приходится пересобрать:
 * SQLite не умеет добавлять внешний ключ к существующей таблице через ALTER.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `biomes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO biomes (id, name, createdAt) VALUES " +
                "($DEFAULT_BIOME_ID, '$DEFAULT_BIOME_NAME', ${System.currentTimeMillis()})",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `venues_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`biomeId` INTEGER NOT NULL, `name` TEXT NOT NULL, `address` TEXT, `note` TEXT, " +
                "`ratingCode` INTEGER, `photoFileName` TEXT, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`biomeId`) REFERENCES `biomes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT INTO venues_new " +
                "(id, biomeId, name, address, note, ratingCode, photoFileName, createdAt, updatedAt) " +
                "SELECT id, $DEFAULT_BIOME_ID, name, address, note, ratingCode, photoFileName, " +
                "createdAt, updatedAt FROM venues",
        )
        db.execSQL("DROP TABLE venues")
        db.execSQL("ALTER TABLE venues_new RENAME TO venues")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_venues_biomeId` ON `venues` (`biomeId`)")
    }
}

/** Биом, в который миграция складывает всё, что было до биомов. */
const val DEFAULT_BIOME_ID = 1L
const val DEFAULT_BIOME_NAME = "Стандартный"

/** Все миграции по порядку — сюда же добавляются будущие. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
