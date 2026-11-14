package fr.florianfish.mywhiskybeer.data.local.entity

import kotlinx.serialization.Serializable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Tronc commun à tous les produits. */
@Entity(
    tableName = "product",
    indices = [Index("type"), Index("name"), Index("barcode")],
)
@Serializable // sauvegarde (data/backup)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ProductType,
    val name: String,
    /** Brasserie, distillerie ou embouteilleur. */
    val producer: String? = null,
    val country: String? = null,
    val region: String? = null,
    /** Degré d'alcool en %. */
    val abv: Double? = null,
    val barcode: String? = null,
    val photoPath: String? = null,
    val tags: List<String> = emptyList(),
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "beer_details",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable // sauvegarde (data/backup)
data class BeerDetailsEntity(
    @PrimaryKey val productId: Long,
    /** IPA, stout, lager, saison… */
    val style: String? = null,
    val ibu: Int? = null,
    /** Blonde, ambrée, brune, noire… */
    val color: String? = null,
    /** Bouteille, canette, pression. */
    val format: String? = null,
)

@Entity(
    tableName = "whisky_details",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable // sauvegarde (data/backup)
data class WhiskyDetailsEntity(
    @PrimaryKey val productId: Long,
    /** Single malt, blended malt, blend, bourbon, rye… */
    val category: String? = null,
    val age: Int? = null,
    val vintage: Int? = null,
    /** Ex-bourbon, sherry, porto, vin… */
    val caskType: String? = null,
    val peat: PeatLevel = PeatLevel.NONE,
    val ppm: Int? = null,
    val caskStrength: Boolean = false,
    val chillFiltered: Boolean? = null,
    val naturalColor: Boolean? = null,
    /** Embouteilleur officiel ou indépendant. */
    val bottler: String? = null,
)

/** Un produit avec ses détails spécifiques (un seul des deux est renseigné). */
data class ProductWithDetails(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val beer: BeerDetailsEntity?,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val whisky: WhiskyDetailsEntity?,
)
