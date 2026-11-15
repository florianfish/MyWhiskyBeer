package fr.florianfish.mywhiskybeer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.florianfish.mywhiskybeer.data.local.AppDatabase
import fr.florianfish.mywhiskybeer.data.local.AromaSeed
import fr.florianfish.mywhiskybeer.data.local.dao.AromaDao
import fr.florianfish.mywhiskybeer.data.local.dao.BackupDao
import fr.florianfish.mywhiskybeer.data.local.dao.BeerStyleDao
import fr.florianfish.mywhiskybeer.data.local.dao.CellarDao
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addCallback(AromaSeed.callback)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

    @Provides fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
    @Provides fun provideTastingDao(db: AppDatabase): TastingDao = db.tastingDao()
    @Provides fun provideAromaDao(db: AppDatabase): AromaDao = db.aromaDao()
    @Provides fun provideCellarDao(db: AppDatabase): CellarDao = db.cellarDao()
    @Provides fun provideBackupDao(db: AppDatabase): BackupDao = db.backupDao()
    @Provides fun provideBeerStyleDao(db: AppDatabase): BeerStyleDao = db.beerStyleDao()
}
