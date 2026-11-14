package fr.florianfish.mywhiskybeer.data.local.entity

import kotlinx.serialization.Serializable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Une dégustation d'un produit. Un même produit peut être goûté plusieurs fois. */
@Entity(
    tableName = "tasting",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("productId"), Index("date")],
)
@Serializable // sauvegarde (data/backup)
data class TastingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val date: Long = System.currentTimeMillis(),
    val place: String? = null,
    /** Note stockée sur 100, affichée selon le type (voir RatingScale). */
    val rating: Int,
    val nose: String? = null,
    val palate: String? = null,
    val finish: String? = null,
    /** Whisky : sec, goutte d'eau, glace. Bière : verre, température. */
    val servedAs: String? = null,
    val price: Double? = null,
    val comment: String? = null,
    val photoPath: String? = null,
)

/** Référentiel d'arômes, propre à chaque type de produit. */
@Entity(
    tableName = "aroma",
    indices = [Index(value = ["productType", "family", "name"], unique = true)],
)
data class AromaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productType: ProductType,
    val family: String,
    val name: String,
)

@Entity(
    tableName = "tasting_aroma",
    primaryKeys = ["tastingId", "aromaId"],
    foreignKeys = [
        ForeignKey(
            entity = TastingEntity::class,
            parentColumns = ["id"],
            childColumns = ["tastingId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AromaEntity::class,
            parentColumns = ["id"],
            childColumns = ["aromaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("aromaId")],
)
data class TastingAromaCrossRef(
    val tastingId: Long,
    val aromaId: Long,
    /** 1 = léger, 2 = marqué, 3 = dominant. */
    val intensity: Int = 1,
)

/** Une dégustation avec son produit et ses arômes, pour l'affichage. */
data class TastingWithProduct(
    @Embedded val tasting: TastingEntity,
    @Relation(parentColumn = "productId", entityColumn = "id")
    val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TastingAromaCrossRef::class,
            parentColumn = "tastingId",
            entityColumn = "aromaId",
        ),
    )
    val aromas: List<AromaEntity>,
)

/** Arôme noté dans une dégustation, avec son intensité. */
data class AromaIntensity(
    val tastingId: Long,
    val intensity: Int,
    @Embedded val aroma: AromaEntity,
)
