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

/** Все миграции по порядку — сюда же добавляются будущие. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
