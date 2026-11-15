package fr.florianfish.mywhiskybeer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.florianfish.mywhiskybeer.data.local.dao.AromaDao
import fr.florianfish.mywhiskybeer.data.local.dao.BackupDao
import fr.florianfish.mywhiskybeer.data.local.dao.BeerStyleDao
import fr.florianfish.mywhiskybeer.data.local.dao.CellarDao
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerStyleEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.TastingAromaCrossRef
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import fr.florianfish.mywhiskybeer.domain.BeerStyles

@Database(
    entities = [
        ProductEntity::class,
        BeerDetailsEntity::class,
        WhiskyDetailsEntity::class,
        TastingEntity::class,
        AromaEntity::class,
        TastingAromaCrossRef::class,
        CellarItemEntity::class,
        BeerStyleEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun tastingDao(): TastingDao
    abstract fun aromaDao(): AromaDao
    abstract fun cellarDao(): CellarDao
    abstract fun backupDao(): BackupDao
    abstract fun beerStyleDao(): BeerStyleDao

    companion object {
        const val NAME = "mywhiskybeer.db"

        /** v2 : nombre de bouteilles en cave. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cellar_item ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * v3 : liste des styles de bière gérée par l'utilisateur. Les styles déjà saisis sont
         * ramenés à la liste (« Hazy IPA » → NEIPA…) puis ajoutés à la liste s'ils n'y sont pas.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `beer_style` (`name` TEXT NOT NULL COLLATE NOCASE, " +
                        "`position` INTEGER NOT NULL, PRIMARY KEY(`name`))",
                )
                val existing = mutableListOf<String>()
                db.query("SELECT DISTINCT style FROM beer_details WHERE style IS NOT NULL").use { c ->
                    while (c.moveToNext()) existing += c.getString(0)
                }
                existing.forEach { old ->
                    val canonical = BeerStyles.canonical(old)
                    if (canonical == null) {
                        db.execSQL("UPDATE beer_details SET style = NULL WHERE style = ?", arrayOf(old))
                    } else if (canonical != old) {
                        db.execSQL("UPDATE beer_details SET style = ? WHERE style = ?", arrayOf(canonical, old))
                    }
                }
                seedBeerStyles(db)
                val used = existing.mapNotNull(BeerStyles::canonical).distinct().filter { it !in BeerStyles.ALL }
                used.forEachIndexed { i, style ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO beer_style (name, position) VALUES (?, ?)",
                        arrayOf(style, BeerStyles.ALL.size + i),
                    )
                }
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        /** Liste de styles par défaut (création de la base, migration). */
        fun seedBeerStyles(db: SupportSQLiteDatabase) {
            BeerStyles.ALL.forEachIndexed { i, style ->
                db.execSQL("INSERT OR IGNORE INTO beer_style (name, position) VALUES (?, ?)", arrayOf(style, i))
            }
        }
    }
}
