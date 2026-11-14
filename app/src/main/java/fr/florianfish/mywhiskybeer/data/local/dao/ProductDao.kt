package fr.florianfish.mywhiskybeer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductDao {

    @Insert
    abstract suspend fun insertProduct(product: ProductEntity): Long

    @Update
    abstract suspend fun updateProduct(product: ProductEntity)

    @Delete
    abstract suspend fun deleteProduct(product: ProductEntity)

    @Upsert
    abstract suspend fun upsertBeerDetails(details: BeerDetailsEntity)

    @Upsert
    abstract suspend fun upsertWhiskyDetails(details: WhiskyDetailsEntity)

    @Transaction
    open suspend fun insertBeer(product: ProductEntity, details: BeerDetailsEntity): Long {
        val id = insertProduct(product.copy(type = ProductType.BEER))
        upsertBeerDetails(details.copy(productId = id))
        return id
    }

    @Transaction
    open suspend fun insertWhisky(product: ProductEntity, details: WhiskyDetailsEntity): Long {
        val id = insertProduct(product.copy(type = ProductType.WHISKY))
        upsertWhiskyDetails(details.copy(productId = id))
        return id
    }

    @Transaction
    @Query("SELECT * FROM product WHERE id = :id")
    abstract fun observeById(id: Long): Flow<ProductWithDetails?>

    @Transaction
    @Query("SELECT * FROM product ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<ProductWithDetails>>

    @Transaction
    @Query("SELECT * FROM product WHERE type = :type ORDER BY name COLLATE NOCASE")
    abstract fun observeByType(type: ProductType): Flow<List<ProductWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM product
        WHERE name LIKE '%' || :query || '%' OR producer LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE
        """,
    )
    abstract fun search(query: String): Flow<List<ProductWithDetails>>

    @Query("SELECT * FROM product WHERE barcode = :barcode LIMIT 1")
    abstract suspend fun findByBarcode(barcode: String): ProductEntity?

    /** Pour l'autocomplétion des producteurs déjà saisis. */
    @Query(
        """
        SELECT DISTINCT producer FROM product
        WHERE type = :type AND producer IS NOT NULL
        ORDER BY producer COLLATE NOCASE
        """,
    )
    abstract fun observeProducers(type: ProductType): Flow<List<String>>
}
