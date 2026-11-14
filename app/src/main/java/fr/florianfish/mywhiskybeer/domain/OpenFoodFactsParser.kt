package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Transforme une réponse de l'API Open Food Facts (v2, /api/v2/product/{code}) en [LabelGuess].
 * Les champs structurés (nom, marque, degré, catégories) priment ; le reste (style, région…)
 * est déduit du texte avec [LabelParser].
 */
object OpenFoodFactsParser {

    /** Champs demandés à l'API, pour une réponse légère. */
    const val FIELDS = "product_name,product_name_fr,brands,nutriments,categories_tags,origins,countries_tags,quantity"

    private val json = Json { ignoreUnknownKeys = true }

    /** null si le produit est inconnu ou la réponse illisible. */
    fun parse(body: String): LabelGuess? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["status"]?.jsonPrimitive?.intOrNull != 1) return null
        val product = root["product"] as? JsonObject ?: return null

        val name = product.string("product_name_fr") ?: product.string("product_name")
        val brand = product.string("brands")?.split(',')?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
        val categories = (product["categories_tags"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            .orEmpty()
        val nutriments = product["nutriments"] as? JsonObject
        val abv = listOf("alcohol_100g", "alcohol", "alcohol_value")
            .firstNotNullOfOrNull { key -> nutriments?.number(key) }
            ?.takeIf { it in 0.5..75.0 }

        val typeFromCategories = when {
            categories.any { it.startsWith("en:whisk") || it.contains("whisky") || it.contains("whiskey") || it.contains("bourbon") } ->
                ProductType.WHISKY
            categories.any { it == "en:beers" || it.startsWith("en:beer") || it.endsWith("-beers") || it.contains("ales") || it.contains("lagers") } ->
                ProductType.BEER
            else -> null
        }

        // Le texte (nom, marque, catégories, origine) passe aussi par l'analyse d'étiquette
        val textLines = buildList {
            name?.let { add(OcrLine(it, 100)) }
            brand?.let { add(OcrLine(it, 50)) }
            product.string("origins")?.let { add(OcrLine(it, 10)) }
            categories.forEach { add(OcrLine(it.substringAfter(':').replace('-', ' '), 5)) }
            (product["countries_tags"] as? JsonArray)?.forEach { c ->
                (c as? JsonPrimitive)?.content?.let { add(OcrLine(it.substringAfter(':').replace('-', ' '), 5)) }
            }
        }
        val fromText = LabelParser.parse(textLines)
        val type = typeFromCategories ?: fromText.type

        return fromText.copy(
            type = type,
            name = name?.let(LabelParser::prettify) ?: fromText.name,
            producer = brand?.let(LabelParser::prettify) ?: fromText.producer,
            abv = abv ?: fromText.abv,
            age = if (type == ProductType.BEER) null else fromText.age,
            style = if (type == ProductType.WHISKY) null else fromText.style,
            category = if (type == ProductType.BEER) null else fromText.category,
        )
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.number(key: String): Double? {
        val p = this[key] as? JsonPrimitive ?: return null
        return p.doubleOrNull ?: p.content.replace(',', '.').toDoubleOrNull()
    }
}

/** Contrôle de la clé d'un code EAN-13 / EAN-8 / UPC-A : évite d'interroger l'API pour un code mal lu. */
object Barcodes {
    fun isValid(code: String): Boolean {
        if (code.length !in setOf(8, 12, 13) || !code.all(Char::isDigit)) return false
        val digits = code.map { it - '0' }
        val body = digits.dropLast(1).reversed()
        val sum = body.mapIndexed { i, d -> if (i % 2 == 0) d * 3 else d }.sum()
        return (10 - sum % 10) % 10 == digits.last()
    }
}
