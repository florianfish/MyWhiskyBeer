package fr.florianfish.mywhiskybeer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerStyleEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.TastingAromaCrossRef
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity

/** Lecture et remplacement complets de la base, pour la sauvegarde et la restauration. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM product") suspend fun products(): List<ProductEntity>
    @Query("SELECT * FROM beer_details") suspend fun beerDetails(): List<BeerDetailsEntity>
    @Query("SELECT * FROM whisky_details") suspend fun whiskyDetails(): List<WhiskyDetailsEntity>
    @Query("SELECT * FROM tasting") suspend fun tastings(): List<TastingEntity>
    @Query("SELECT * FROM tasting_aroma") suspend fun tastingAromas(): List<TastingAromaCrossRef>
    @Query("SELECT * FROM aroma") suspend fun aromas(): List<AromaEntity>
    @Query("SELECT * FROM cellar_item") suspend fun cellar(): List<CellarItemEntity>
    @Query("SELECT * FROM beer_style") suspend fun beerStyles(): List<BeerStyleEntity>

    // Suppression dans l'ordre des dépendances (sans compter sur les cascades)
    @Query("DELETE FROM tasting_aroma") suspend fun clearTastingAromas()
    @Query("DELETE FROM tasting") suspend fun clearTastings()
    @Query("DELETE FROM cellar_item") suspend fun clearCellar()
    @Query("DELETE FROM beer_details") suspend fun clearBeerDetails()
    @Query("DELETE FROM whisky_details") suspend fun clearWhiskyDetails()
    @Query("DELETE FROM product") suspend fun clearProducts()
    @Query("DELETE FROM beer_style") suspend fun clearBeerStyles()

    @Insert suspend fun insertProducts(items: List<ProductEntity>)
    @Insert suspend fun insertBeerDetails(items: List<BeerDetailsEntity>)
    @Insert suspend fun insertWhiskyDetails(items: List<WhiskyDetailsEntity>)
    @Insert suspend fun insertTastings(items: List<TastingEntity>)
    @Insert suspend fun insertTastingAromas(items: List<TastingAromaCrossRef>)
    @Insert suspend fun insertCellar(items: List<CellarItemEntity>)
    @Insert suspend fun insertAroma(aroma: AromaEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBeerStyles(items: List<BeerStyleEntity>)
}
