package fr.florianfish.mywhiskybeer.data.backup

import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Contenu d'une sauvegarde. Les chemins de photos y sont réduits au nom de fichier
 * (les photos sont dans le dossier « photos/ » du zip).
 */
@Serializable
data class BackupData(
    /** Version du format de sauvegarde (pas de la base). */
    val format: Int = FORMAT,
    val createdAt: Long,
    val appVersion: String,
    val products: List<ProductEntity>,
    val beerDetails: List<BeerDetailsEntity>,
    val whiskyDetails: List<WhiskyDetailsEntity>,
    val tastings: List<TastingEntity>,
    /** Arômes repérés par type/famille/nom : les identifiants du référentiel peuvent changer d'une installation à l'autre. */
    val aromas: List<AromaRef>,
    val cellar: List<CellarItemEntity>,
) {
    companion object {
        const val FORMAT = 1
    }
}

@Serializable
data class AromaRef(
    val tastingId: Long,
    val type: ProductType,
    val family: String,
    val name: String,
    val intensity: Int,
)

/** Ce que contient une sauvegarde, pour l'afficher. */
data class BackupSummary(val products: Int, val tastings: Int, val bottles: Int, val photos: Int) {
    val text: String
        get() = listOf(
            plural(products, "produit"),
            plural(tastings, "dégustation"),
            plural(bottles, "bouteille"),
            plural(photos, "photo"),
        ).joinToString(", ")

    private fun plural(n: Int, word: String) = "$n $word" + if (n > 1) "s" else ""
}

object BackupCodec {
    const val JSON_ENTRY = "mywhiskybeer.json"
    const val PHOTOS_DIR = "photos/"

    private val json = Json {
        ignoreUnknownKeys = true // une sauvegarde d'une version plus récente reste lisible
        encodeDefaults = true
    }

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    fun decode(text: String): BackupData {
        val data = json.decodeFromString(BackupData.serializer(), text)
        require(data.format <= BackupData.FORMAT) {
            "Sauvegarde créée par une version plus récente de l'appli (format ${data.format})"
        }
        return data
    }

    /** Nom de fichier accepté dans le zip : pas de dossier, pas de « .. ». */
    fun isSafePhotoName(name: String): Boolean = name.matches(Regex("[A-Za-z0-9._-]{1,128}")) && name != "." && name != ".."

    /** Remplace chaque chemin de photo par le résultat de [transform] (null = pas de photo). */
    fun mapPhotos(data: BackupData, transform: (String) -> String?): BackupData = data.copy(
        products = data.products.map { p -> p.copy(photoPath = p.photoPath?.let(transform)) },
        tastings = data.tastings.map { t -> t.copy(photoPath = t.photoPath?.let(transform)) },
    )

    fun summary(data: BackupData) = BackupSummary(
        products = data.products.size,
        tastings = data.tastings.size,
        bottles = data.cellar.filter { it.status == CellarStatus.IN_STOCK || it.status == CellarStatus.OPENED }.sumOf { it.quantity },
        photos = (data.products.mapNotNull { it.photoPath } + data.tastings.mapNotNull { it.photoPath }).toSet().size,
    )
}
