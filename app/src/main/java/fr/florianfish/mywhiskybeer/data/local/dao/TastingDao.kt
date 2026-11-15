package fr.florianfish.mywhiskybeer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.florianfish.mywhiskybeer.data.local.entity.AromaIntensity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.TastingAromaCrossRef
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.TastingWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TastingDao {

    @Insert
    abstract suspend fun insertTasting(tasting: TastingEntity): Long

    @Update
    abstract suspend fun updateTasting(tasting: TastingEntity)

    @Delete
    abstract suspend fun deleteTasting(tasting: TastingEntity)

    @Insert
    abstract suspend fun insertAromaRefs(refs: List<TastingAromaCrossRef>)

    @Query("DELETE FROM tasting_aroma WHERE tastingId = :tastingId")
    abstract suspend fun clearAromaRefs(tastingId: Long)

    /** Crée ou met à jour une dégustation avec ses arômes (aromaId → intensité). */
    @Transaction
    open suspend fun save(tasting: TastingEntity, aromas: Map<Long, Int>): Long {
        val id = if (tasting.id == 0L) {
            insertTasting(tasting)
        } else {
            updateTasting(tasting)
            clearAromaRefs(tasting.id)
            tasting.id
        }
        insertAromaRefs(aromas.map { (aromaId, intensity) -> TastingAromaCrossRef(id, aromaId, intensity) })
        return id
    }

    @Transaction
    @Query("SELECT * FROM tasting ORDER BY date DESC LIMIT :limit")
    abstract fun observeLatest(limit: Int = 20): Flow<List<TastingWithProduct>>

    /** Dernières dégustations d'un seul type de produit (filtre de l'accueil). */
    @Transaction
    @Query(
        """
        SELECT t.* FROM tasting t JOIN product p ON p.id = t.productId
        WHERE p.type = :type ORDER BY t.date DESC LIMIT :limit
        """,
    )
    abstract fun observeLatestByType(type: ProductType, limit: Int = 20): Flow<List<TastingWithProduct>>

    /** Dernières dégustations de bières d'un style (filtre par style de l'accueil). */
    @Transaction
    @Query(
        """
        SELECT t.* FROM tasting t JOIN beer_details b ON b.productId = t.productId
        WHERE b.style = :style COLLATE NOCASE ORDER BY t.date DESC LIMIT :limit
        """,
    )
    abstract fun observeLatestByBeerStyle(style: String, limit: Int = 20): Flow<List<TastingWithProduct>>

    @Transaction
    @Query("SELECT * FROM tasting WHERE productId = :productId ORDER BY date DESC")
    abstract fun observeForProduct(productId: Long): Flow<List<TastingWithProduct>>

    /** Toutes les dégustations (statistiques). */
    @Query("SELECT * FROM tasting")
    abstract fun observeAll(): Flow<List<TastingEntity>>

    /** Tous les arômes notés, toutes dégustations confondues (profil aromatique global). */
    @Query("SELECT ta.tastingId, ta.intensity, a.* FROM tasting_aroma ta JOIN aroma a ON a.id = ta.aromaId")
    abstract fun observeAllAromaIntensities(): Flow<List<AromaIntensity>>

    @Query("SELECT AVG(rating) FROM tasting WHERE productId = :productId")
    abstract fun observeAverageRating(productId: Long): Flow<Double?>

    /** Dernier lieu utilisé, pour pré-remplir la saisie. */
    @Query("SELECT place FROM tasting WHERE place IS NOT NULL ORDER BY date DESC LIMIT 1")
    abstract suspend fun lastPlace(): String?

    /** Arômes et intensités de toutes les dégustations d'un produit (profil aromatique). */
    @Query(
        """
        SELECT ta.tastingId, ta.intensity, a.* FROM tasting_aroma ta
        JOIN aroma a ON a.id = ta.aromaId
        JOIN tasting t ON t.id = ta.tastingId
        WHERE t.productId = :productId
        """,
    )
    abstract fun observeAromaIntensities(productId: Long): Flow<List<AromaIntensity>>

    @Query("SELECT COUNT(*) FROM tasting")
    abstract fun observeCount(): Flow<Int>

    @Query("SELECT * FROM tasting WHERE id = :id")
    abstract suspend fun getById(id: Long): TastingEntity?

    /** Arômes cochés d'une dégustation, avec leur intensité (pour la modifier). */
    @Query("SELECT * FROM tasting_aroma WHERE tastingId = :tastingId")
    abstract suspend fun aromaRefs(tastingId: Long): List<TastingAromaCrossRef>

    @Query("SELECT COUNT(*) FROM tasting WHERE photoPath = :path")
    abstract suspend fun countPhotoUses(path: String): Int
}
