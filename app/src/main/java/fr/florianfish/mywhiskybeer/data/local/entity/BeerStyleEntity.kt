package fr.florianfish.mywhiskybeer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Style de bière de la liste gérée par l'utilisateur (Réglages › Styles de bière).
 * Initialisée avec [fr.florianfish.mywhiskybeer.domain.BeerStyles.ALL]. `beer_details.style`
 * reste du texte : tout style utilisé est ajouté à la liste à l'enregistrement.
 */
@Entity(tableName = "beer_style")
@Serializable // sauvegarde (data/backup)
data class BeerStyleEntity(
    @PrimaryKey @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    /** Ordre d'affichage. */
    val position: Int,
)

/** Style et nombre de bières qui l'utilisent. */
data class BeerStyleUsage(val style: String, val count: Int)
