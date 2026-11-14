package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.PeatLevel
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType

/**
 * Champs d'un nouveau produit, saisis sous forme de texte (convertis à l'enregistrement).
 * Partagé par la saisie d'une dégustation et l'ajout d'une bouteille en cave.
 */
data class ProductForm(
    val type: ProductType = ProductType.WHISKY,
    val name: String = "",
    val producer: String = "",
    val country: String = "",
    val region: String = "",
    val abv: String = "",
    // Bière
    val style: String = "",
    val ibu: String = "",
    val color: String = "",
    // Whisky
    val category: String = "",
    val age: String = "",
    val caskType: String = "",
    val peat: PeatLevel = PeatLevel.NONE,
    val caskStrength: Boolean = false,
) {
    /**
     * Complète le formulaire avec ce qui a été lu sur l'étiquette.
     * Seuls les champs vides sont remplis : ce que l'utilisateur a déjà tapé est conservé.
     */
    fun withGuess(guess: LabelGuess): ProductForm {
        val fresh = name.isBlank()
        val newType = if (fresh && guess.type != null) guess.type else type
        return copy(
            type = newType,
            name = name.ifBlank { guess.name.orEmpty() },
            producer = producer.ifBlank { guess.producer.orEmpty() },
            country = country.ifBlank { guess.country.orEmpty() },
            region = region.ifBlank { guess.region.orEmpty() },
            abv = abv.ifBlank { guess.abv?.let(::formatDecimal).orEmpty() },
            style = style.ifBlank { guess.style.orEmpty() },
            category = category.ifBlank { guess.category.orEmpty() },
            age = age.ifBlank { guess.age?.toString().orEmpty() },
            caskType = caskType.ifBlank { guess.caskType.orEmpty() },
            caskStrength = caskStrength || guess.caskStrength,
        )
    }

    /** Vrai si l'étiquette a rempli des champs de « Plus de détails » (pour les déplier). */
    fun hasDetails(): Boolean =
        listOf(country, region, abv, style, ibu, color, category, age, caskType).any { it.isNotBlank() } || caskStrength
}

/** « 46.0 » → « 46 », « 5.8 » → « 5,8 ». */
fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')

/** Accepte la virgule française : « 46,3 » → 46.3. */
fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

fun String.blankToNull(): String? = trim().ifEmpty { null }
