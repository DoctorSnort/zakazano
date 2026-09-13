package kz.chaykin.zakazano.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Переезд первой версии базы на вторую. Проверка не формальная: у приложения уже есть
 * пользователь с записями, и обновление не должно их потерять.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ZakazanoDatabase::class.java,
    )

    @Test
    fun записи_первой_версии_переживают_переезд_на_вторую() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO venues (name, address, note, ratingCode, createdAt, updatedAt)
                VALUES ('Кафе на Ёлочной', 'улица Ёлочная, 1', 'шумно', 2, 100, 200)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO items (venueId, name, kind, ratingCode, priceMinor, comment, createdAt, updatedAt)
                VALUES (1, 'Плов', 'DISH', 3, 250050, 'мяса много', 100, 200)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO items (venueId, name, kind, ratingCode, priceMinor, comment, createdAt, updatedAt)
                VALUES (1, 'Морс', 'DRINK', 2, 50000, NULL, 100, 200)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        db.query("SELECT name, address, note, ratingCode, photoFileName FROM venues").use { c ->
            assertTrue("заведение пропало при миграции", c.moveToFirst())
            assertEquals("Кафе на Ёлочной", c.getString(0))
            assertEquals("улица Ёлочная, 1", c.getString(1))
            assertEquals("шумно", c.getString(2))
            assertEquals(2, c.getInt(3))
            assertTrue("у старого заведения не должно быть фотографии", c.isNull(4))
            assertEquals(1, c.count)
        }

        db.query("SELECT name, kind, drinkType, ratingCode, priceMinor FROM items ORDER BY name").use { c ->
            assertEquals(2, c.count)

            assertTrue(c.moveToFirst())
            assertEquals("Морс", c.getString(0))
            assertEquals("DRINK", c.getString(1))
            assertTrue("у старого напитка вида быть не должно", c.isNull(2))

            assertTrue(c.moveToNext())
            assertEquals("Плов", c.getString(0))
            assertEquals("DISH", c.getString(1))
            assertEquals(3, c.getInt(3))
            assertEquals(250050L, c.getLong(4))
        }
    }

    @Test
    fun новые_колонки_принимают_значения_после_миграции() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO venues (name, address, note, ratingCode, createdAt, updatedAt)
                VALUES ('Бар', NULL, NULL, NULL, 1, 1)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO items (venueId, name, kind, ratingCode, priceMinor, comment, createdAt, updatedAt)
                VALUES (1, 'Негрони', 'DRINK', 3, NULL, NULL, 1, 1)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)
        db.execSQL("UPDATE venues SET photoFileName = 'venue.jpg' WHERE id = 1")
        db.execSQL("UPDATE items SET drinkType = 'COCKTAIL' WHERE id = 1")

        db.query("SELECT photoFileName FROM venues").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("venue.jpg", c.getString(0))
        }
        db.query("SELECT drinkType FROM items").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("COCKTAIL", c.getString(0))
        }
    }

    @Test
    fun пустая_база_второй_версии_создаётся_без_миграции() {
        helper.createDatabase("fresh.db", 2).use { db ->
            db.query("SELECT drinkType FROM items").use { c -> assertEquals(0, c.count) }
            db.query("SELECT photoFileName FROM venues").use { c -> assertEquals(0, c.count) }
            assertNull(null)
        }
    }
}
