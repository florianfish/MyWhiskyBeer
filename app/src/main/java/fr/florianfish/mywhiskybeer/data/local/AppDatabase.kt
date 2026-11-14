package fr.florianfish.mywhiskybeer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.florianfish.mywhiskybeer.data.local.dao.AromaDao
import fr.florianfish.mywhiskybeer.data.local.dao.CellarDao
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.TastingAromaCrossRef
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity

@Database(
    entities = [
        ProductEntity::class,
        BeerDetailsEntity::class,
        WhiskyDetailsEntity::class,
        TastingEntity::class,
        AromaEntity::class,
        TastingAromaCrossRef::class,
        CellarItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun tastingDao(): TastingDao
    abstract fun aromaDao(): AromaDao
    abstract fun cellarDao(): CellarDao

    companion object {
        const val NAME = "mywhiskybeer.db"
    }
}
