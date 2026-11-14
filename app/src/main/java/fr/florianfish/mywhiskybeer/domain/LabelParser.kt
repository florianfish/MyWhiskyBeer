package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import java.text.Normalizer

/** Ligne de texte lue sur l'étiquette, avec la hauteur de son cadre (en px) : plus c'est grand, plus c'est important. */
data class OcrLine(val text: String, val height: Int)

/** Ce qu'on a pu deviner d'une étiquette. Tout est facultatif. */
data class LabelGuess(
    val type: ProductType? = null,
    val name: String? = null,
    val producer: String? = null,
    val abv: Double? = null,
    val age: Int? = null,
    val style: String? = null,
    val category: String? = null,
    val region: String? = null,
    val country: String? = null,
    val caskType: String? = null,
    val caskStrength: Boolean = false,
)

/**
 * Devine les champs d'un produit à partir du texte lu sur l'étiquette.
 * Heuristiques simples : mots-clés, expressions régulières, et le plus gros texte pour le nom.
 */
object LabelParser {

    private val ignoreCase = setOf(RegexOption.IGNORE_CASE)

    private val abvPercent = Regex("""(\d{1,2}(?:[.,]\d{1,2})?)\s*%""")
    private val abvPrefix = Regex("""\b(?:alc|abv)\.?\s*:?\s*(\d{1,2}(?:[.,]\d{1,2})?)""", ignoreCase)
    private val age = Regex("""\b(\d{1,2})\s*(?:years?|yrs?|ans|y\.?o\.?|year[- ]old|jahre)\b""", ignoreCase)
    private val aged = Regex("""\baged\s+(\d{1,2})\b""", ignoreCase)
    private val volume = Regex("""\b\d+(?:[.,]\d+)?\s*(?:cl|ml|l)\b""", ignoreCase)

    private val whiskyWords = listOf(
        "whisky", "whiskey", "scotch", "single malt", "malt", "bourbon", "rye", "distillery",
        "distillerie", "distilled", "distillee", "matured", "cask", "fut", "islay", "speyside", "highland",
    )
    private val beerWords = listOf(
        "beer", "biere", "bier", "ale", "ipa", "lager", "stout", "porter", "pils", "pilsner", "brasserie",
        "brewery", "brewing", "brewed", "brouwerij", "saison", "tripel", "triple", "blanche", "houblon", "hops",
    )
    private val producerWords = Regex(
        """\b(distillery|distillerie|distilleries|brewery|breweries|brasserie|brewing(?:\s+co(?:mpany)?\.?)?|brouwerij|brauerei)\b""",
        ignoreCase,
    )

    /** Styles de bière : du plus précis au plus général. */
    private val beerStyles = listOf(
        "new england ipa" to "NEIPA", "neipa" to "NEIPA", "hazy ipa" to "Hazy IPA",
        "west coast ipa" to "West Coast IPA", "double ipa" to "Double IPA", "session ipa" to "Session IPA",
        "imperial stout" to "Imperial stout", "pale ale" to "Pale ale", "brown ale" to "Brown ale",
        "red ale" to "Red ale", "barley wine" to "Barley wine", "berliner weisse" to "Berliner weisse",
        "ipa" to "IPA", "stout" to "Stout", "porter" to "Porter", "pilsner" to "Pils", "pils" to "Pils",
        "lager" to "Lager", "saison" to "Saison", "tripel" to "Triple", "triple" to "Triple",
        "quadrupel" to "Quadruple", "dubbel" to "Double", "blanche" to "Blanche", "witbier" to "Blanche",
        "hefeweizen" to "Weizen", "weizen" to "Weizen", "gose" to "Gose", "sour" to "Sour", "bock" to "Bock",
        "kolsch" to "Kölsch", "ambree" to "Ambrée", "amber" to "Ambrée",
    )

    private val whiskyCategories = listOf(
        "single malt" to "Single malt", "blended malt" to "Blended malt", "single grain" to "Single grain",
        "single pot still" to "Single pot still", "blended" to "Blend", "bourbon" to "Bourbon", "rye" to "Rye",
    )

    /** Mot-clé → (région, pays). */
    private val places = listOf(
        "islay" to ("Islay" to "Écosse"), "speyside" to ("Speyside" to "Écosse"),
        "highland" to ("Highlands" to "Écosse"), "lowland" to ("Lowlands" to "Écosse"),
        "campbeltown" to ("Campbeltown" to "Écosse"), "orkney" to ("Islands" to "Écosse"),
        "skye" to ("Islands" to "Écosse"), "kentucky" to ("Kentucky" to "États-Unis"),
        "tennessee" to ("Tennessee" to "États-Unis"), "bretagne" to ("Bretagne" to "France"),
    )
    private val countries = listOf(
        "scotch" to "Écosse", "scotland" to "Écosse", "ecosse" to "Écosse", "ireland" to "Irlande", "irish" to "Irlande",
        "japan" to "Japon", "japanese" to "Japon", "belgium" to "Belgique", "belgian" to "Belgique",
        "belgique" to "Belgique", "france" to "France", "francaise" to "France", "germany" to "Allemagne",
        "usa" to "États-Unis", "american" to "États-Unis", "taiwan" to "Taïwan", "india" to "Inde",
    )

    private val casks = listOf(
        "oloroso" to "Sherry oloroso", "pedro ximenez" to "Sherry PX", "px cask" to "Sherry PX",
        "sherry" to "Sherry", "port cask" to "Porto", "port wood" to "Porto", "porto" to "Porto",
        "madeira" to "Madère", "sauternes" to "Sauternes", "rum cask" to "Rhum", "wine cask" to "Vin",
        "bourbon cask" to "Ex-bourbon", "bourbon barrel" to "Ex-bourbon", "virgin oak" to "Chêne neuf",
    )

    private val caskStrength = Regex("""\b(cask strength|brut de fut|barrel proof|full proof)\b""")

    /** Mots qui ne font pas un nom à eux seuls. */
    private val genericWords = setOf(
        "single", "malt", "scotch", "whisky", "whiskey", "blended", "bourbon", "straight", "kentucky", "product",
        "of", "the", "scotland", "distilled", "matured", "bottled", "beer", "biere", "ale", "years", "year", "old",
        "aged", "ans", "cask", "strength", "non", "chill", "filtered", "natural", "colour", "color", "vol", "alc",
        "cl", "ml", "l", "and", "by", "in", "de", "la", "le", "du", "et", "ipa", "lager", "craft", "brewed",
        "distillery", "brewery", "brasserie", "edition", "limited", "since", "est", "estd", "established",
    )

    fun parse(lines: List<OcrLine>): LabelGuess {
        val cleaned = lines.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
        if (cleaned.isEmpty()) return LabelGuess()
        val all = cleaned.joinToString("\n") { it.text }
        val folded = fold(all)

        val whiskyScore = whiskyWords.count { containsWord(folded, it) }
        val beerScore = beerWords.count { containsWord(folded, it) }
        val type = when {
            whiskyScore > beerScore -> ProductType.WHISKY
            beerScore > whiskyScore -> ProductType.BEER
            else -> null
        }

        val abv = (abvPercent.findAll(all) + abvPrefix.findAll(all))
            .mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }
            .firstOrNull { it in 0.5..75.0 }

        val ageValue = (age.findAll(all) + aged.findAll(all))
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .firstOrNull { it in 3..60 }

        val producerLine = cleaned.firstOrNull { producerWords.containsMatchIn(fold(it.text)) }
        // « ARDBEG DISTILLERY » → « Ardbeg » ; si rien ne reste, on garde la ligne entière
        val producer = producerLine?.let { line ->
            val base = producerWords.replace(line.text, " ")
                .replace(Regex("""^\s*the\s+""", RegexOption.IGNORE_CASE), "")
                .trim(' ', ',', '.', '-', '&')
            prettify(base.ifBlank { line.text })
        }

        val name = cleaned
            .filter { it !== producerLine && isNameCandidate(it.text) }
            .maxByOrNull { it.height }
            ?.let { prettify(it.text) }

        val style = if (type != ProductType.WHISKY) beerStyles.firstOrNull { containsWord(folded, it.first) }?.second else null
        val category = if (type != ProductType.BEER) whiskyCategories.firstOrNull { containsWord(folded, it.first) }?.second else null
        val place = places.firstOrNull { containsWord(folded, it.first) }?.second
        val country = place?.second ?: countries.firstOrNull { containsWord(folded, it.first) }?.second
        val cask = if (type != ProductType.BEER) casks.firstOrNull { containsWord(folded, it.first) }?.second else null

        return LabelGuess(
            type = type,
            name = name,
            producer = producer,
            abv = abv,
            age = if (type == ProductType.BEER) null else ageValue,
            style = style,
            category = category,
            region = place?.first,
            country = country,
            caskType = cask,
            caskStrength = type != ProductType.BEER && caskStrength.containsMatchIn(folded),
        )
    }

    /** Ligne qui pourrait être le nom : du texte, pas un degré, un âge, un volume ni uniquement des mots génériques. */
    private fun isNameCandidate(text: String): Boolean {
        if (abvPercent.containsMatchIn(text) || age.containsMatchIn(text) || volume.containsMatchIn(text)) return false
        val words = fold(text).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        val meaningful = words.filter { it !in genericWords && it.any(Char::isLetter) }
        return meaningful.isNotEmpty() && meaningful.sumOf { it.length } >= 3
    }

    /** « ARDBEG » → « Ardbeg » ; un texte déjà en casse mixte est laissé tel quel. */
    internal fun prettify(text: String): String {
        val t = text.trim().replace(Regex("\\s+"), " ")
        val letters = t.filter(Char::isLetter)
        if (letters.isEmpty() || letters.any(Char::isLowerCase)) return t
        return t.lowercase().split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase() }
        }
    }

    private fun containsWord(folded: String, word: String): Boolean =
        Regex("""(^|[^a-z])${Regex.escape(word)}($|[^a-z])""").containsMatchIn(folded)

    /** Minuscules sans accents, pour comparer. */
    internal fun fold(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}
