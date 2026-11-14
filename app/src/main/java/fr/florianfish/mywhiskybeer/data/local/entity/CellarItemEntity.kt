package fr.florianfish.mywhiskybeer.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

/** État d'un produit dans la cave (ou la wishlist). */
@Entity(
    tableName = "cellar_item",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CellarItemEntity(
    @PrimaryKey val productId: Long,
    val status: CellarStatus,
    /** Niveau restant en % (whisky surtout). */
    val fillLevel: Int? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: Double? = null,
    val shop: String? = null,
)

data class CellarItemWithProduct(
    @Embedded val item: CellarItemEntity,
    @Relation(parentColumn = "productId", entityColumn = "id")
    val product: ProductEntity,
)
