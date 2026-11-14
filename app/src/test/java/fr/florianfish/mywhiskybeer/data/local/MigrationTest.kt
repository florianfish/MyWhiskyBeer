package fr.florianfish.mywhiskybeer.data.local

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests des migrations Room, sur la JVM avec Robolectric (pas besoin d'émulateur).
 * Les schémas de chaque version (app/schemas) sont lus en assets du build de debug.
 *
 * Pour chaque nouvelle migration N → N+1 : ajouter un test qui remplit la base en version N
 * avec du SQL brut, migre, et vérifie que les données sont intactes.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class) // Application simple : pas besoin de Hilt ici
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration1Vers2ConserveLesDonnees() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL("INSERT INTO product (id, type, name, producer, tags, created_at) VALUES (1, 'WHISKY', 'Lagavulin 16', 'Lagavulin', 'islay', 1000)")
            db.execSQL("INSERT INTO product (id, type, name, tags, created_at) VALUES (2, 'BEER', 'Zinnebir', '', 2000)")
            db.execSQL("INSERT INTO whisky_details (productId, age, peat, caskStrength) VALUES (1, 16, 'HEAVY', 0)")
            db.execSQL("INSERT INTO beer_details (productId, style) VALUES (2, 'Blonde')")
            db.execSQL("INSERT INTO tasting (id, productId, date, rating, nose) VALUES (10, 1, 3000, 92, 'Fumée')")
            db.execSQL("INSERT INTO cellar_item (productId, status, fillLevel, purchasePrice, shop) VALUES (1, 'OPENED', 40, 89.9, 'La Maison du Whisky')")
            db.execSQL("INSERT INTO cellar_item (productId, status) VALUES (2, 'WISHLIST')")
        }

        helper.runMigrationsAndValidate(dbName, 2, true, AppDatabase.MIGRATION_1_2).use { db ->
            // Nouvelle colonne : une bouteille par défaut, le reste intact
            db.query("SELECT status, fillLevel, purchasePrice, shop, quantity FROM cellar_item WHERE productId = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("OPENED", c.getString(0))
                assertEquals(40, c.getInt(1))
                assertEquals(89.9, c.getDouble(2), 0.0)
                assertEquals("La Maison du Whisky", c.getString(3))
                assertEquals(1, c.getInt(4))
            }
            db.query("SELECT quantity FROM cellar_item WHERE productId = 2").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM product").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
            }
            db.query("SELECT rating, nose FROM tasting WHERE id = 10").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(92, c.getInt(0))
                assertEquals("Fumée", c.getString(1))
            }
            db.query("SELECT age, peat FROM whisky_details WHERE productId = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(16, c.getInt(0))
                assertEquals("HEAVY", c.getString(1))
            }
        }
    }

    /**
     * Depuis la toute première version, la chaîne complète des migrations doit mener à la version
     * actuelle, avec un schéma identique à celui des entités (Room le vérifie à l'ouverture).
     */
    @Test
    fun toutesLesMigrationsMenentALaVersionActuelle() {
        helper.createDatabase(dbName, 1).close()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            AppDatabase::class.java,
            dbName,
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            db.openHelper.writableDatabase // déclenche migrations + validation
        } finally {
            db.close()
        }
    }
}
