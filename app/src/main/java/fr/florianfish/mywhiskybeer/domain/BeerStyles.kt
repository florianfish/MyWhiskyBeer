package fr.florianfish.mywhiskybeer.domain

/**
 * Styles de bière reconnus, pour choisir, filtrer, regrouper et compter.
 * Le style reste stocké en texte (`beer_details.style`) : [canonical] ramène les variantes
 * (« Hazy IPA », « tripel », « witbier »…) au style de la liste ; un style inconnu est gardé tel quel.
 */
object BeerStyles {

    /** Tous les styles, dans l'ordre d'affichage (famille par famille). */
    val ALL = listOf(
        // IPA et ales houblonnées
        "IPA", "NEIPA", "Double IPA", "West Coast IPA", "Session IPA", "Pale ale",
        // Blondes et blanches
        "Lager", "Pils", "Blonde", "Blanche", "Weizen", "Kölsch",
        // Ambrées et brunes
        "Ambrée", "Red ale", "Brown ale", "Bock",
        // Noires
        "Stout", "Imperial stout", "Porter",
        // Acides
        "Sour", "Gose", "Berliner weisse", "Fruitée",
        // Belges et fermières
        "Saison", "Triple", "Double", "Quadruple",
        // Fortes
        "Barley wine",
    )

    /** Les plus courants, proposés directement en pastilles. */
    val POPULAR = listOf("IPA", "NEIPA", "Pale ale", "Lager", "Blonde", "Blanche", "Ambrée", "Stout", "Sour", "Triple")

    /** Variantes (sans accents, minuscules) → style de la liste. */
    private val aliases: Map<String, String> = mapOf(
        "new england ipa" to "NEIPA", "ne ipa" to "NEIPA", "hazy ipa" to "NEIPA", "hazy" to "NEIPA",
        "dipa" to "Double IPA", "imperial ipa" to "Double IPA", "tipa" to "Double IPA",
        "wc ipa" to "West Coast IPA", "west coast" to "West Coast IPA",
        "apa" to "Pale ale", "american pale ale" to "Pale ale",
        "pilsner" to "Pils", "pilsener" to "Pils", "lager blonde" to "Lager", "helles" to "Lager",
        "blond" to "Blonde", "blonde ale" to "Blonde",
        "witbier" to "Blanche", "wit" to "Blanche", "white" to "Blanche", "biere blanche" to "Blanche",
        "hefeweizen" to "Weizen", "weissbier" to "Weizen", "weisse" to "Weizen", "weissbier hell" to "Weizen",
        "kolsch" to "Kölsch",
        "amber" to "Ambrée", "amber ale" to "Ambrée", "ambree" to "Ambrée", "biere ambree" to "Ambrée",
        "irish red" to "Red ale", "rousse" to "Red ale",
        "brune" to "Brown ale", "brown" to "Brown ale",
        "doppelbock" to "Bock",
        "russian imperial stout" to "Imperial stout", "ris" to "Imperial stout",
        "milk stout" to "Stout", "dry stout" to "Stout", "oatmeal stout" to "Stout",
        "sour ale" to "Sour", "fruited sour" to "Sour", "lambic" to "Sour", "gueuze" to "Sour", "kriek" to "Fruitée",
        "fruit beer" to "Fruitée", "fruitee" to "Fruitée", "biere fruitee" to "Fruitée",
        "farmhouse" to "Saison", "farmhouse ale" to "Saison",
        "tripel" to "Triple", "dubbel" to "Double", "quadrupel" to "Quadruple", "quad" to "Quadruple",
        "barleywine" to "Barley wine",
    )

    private val byFolded: Map<String, String> = ALL.associateBy { LabelParser.fold(it) }

    /** Style de la liste correspondant au texte, sinon le texte nettoyé ; null si vide. */
    fun canonical(style: String?): String? {
        val trimmed = style?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val folded = LabelParser.fold(trimmed).replace(Regex("\\s+"), " ")
        return byFolded[folded] ?: aliases[folded] ?: trimmed
    }

    /** Position dans [ALL] pour trier les styles ; les styles inconnus en fin, par ordre alphabétique. */
    fun order(style: String): Int = ALL.indexOf(style).let { if (it < 0) ALL.size else it }
}
