package fr.florianfish.mywhiskybeer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Transaction
    @Query("SELECT * FROM tasting WHERE productId = :productId ORDER BY date DESC")
    abstract fun observeForProduct(productId: Long): Flow<List<TastingWithProduct>>

    @Query("SELECT AVG(rating) FROM tasting WHERE productId = :productId")
    abstract fun observeAverageRating(productId: Long): Flow<Double?>

    /** Dernier lieu utilisé, pour pré-remplir la saisie. */
    @Query("SELECT place FROM tasting WHERE place IS NOT NULL ORDER BY date DESC LIMIT 1")
    abstract suspend fun lastPlace(): String?
}
