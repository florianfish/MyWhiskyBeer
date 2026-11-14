package fr.florianfish.mywhiskybeer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemWithProduct
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CellarDao {
    @Upsert
    suspend fun upsert(item: CellarItemEntity)

    @Delete
    suspend fun delete(item: CellarItemEntity)

    @Query("SELECT * FROM cellar_item WHERE status = :status")
    fun observeByStatus(status: CellarStatus): Flow<List<CellarItemEntity>>

    @Query("SELECT * FROM cellar_item WHERE productId = :productId")
    suspend fun getForProduct(productId: Long): CellarItemEntity?

    @Query("SELECT * FROM cellar_item WHERE productId = :productId")
    fun observeForProduct(productId: Long): Flow<CellarItemEntity?>

    @Transaction
    @Query("SELECT * FROM cellar_item")
    fun observeAllWithProduct(): Flow<List<CellarItemWithProduct>>

    @Query("DELETE FROM cellar_item WHERE productId = :productId")
    suspend fun deleteForProduct(productId: Long)
}
