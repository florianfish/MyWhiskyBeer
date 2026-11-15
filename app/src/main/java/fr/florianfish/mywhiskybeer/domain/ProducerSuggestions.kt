package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProducerInfo
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType

/** Suggestions de distilleries / brasseries déjà connues pendant la saisie du producteur. */
object ProducerSuggestions {

    const val MAX = 4

    /**
     * Producteurs du même type dont le nom contient le texte saisi (sans accents ni casse),
     * dédoublonnés. Pays et région : ceux du produit le plus récent qui les renseigne.
     * Rien si moins de 2 lettres, ou si le texte est déjà exactement un producteur connu.
     * [known] est trié du produit le plus récent au plus ancien.
     */
    fun match(known: List<ProducerInfo>, type: ProductType, typed: String): List<ProducerInfo> {
        val query = LabelParser.fold(typed).trim()
        if (query.length < 2) return emptyList()
        val sameType = known.filter { it.type == type }
        val merged = sameType
            .groupBy { LabelParser.fold(it.producer).trim() }
            .map { (_, infos) ->
                infos.first().copy(
                    country = infos.firstNotNullOfOrNull { it.country?.takeIf(String::isNotBlank) },
                    region = infos.firstNotNullOfOrNull { it.region?.takeIf(String::isNotBlank) },
                )
            }
        if (merged.any { LabelParser.fold(it.producer).trim() == query }) return emptyList()
        return merged
            .filter { query in LabelParser.fold(it.producer) }
            // Ceux qui commencent par le texte saisi d'abord
            .sortedWith(compareBy<ProducerInfo> { !LabelParser.fold(it.producer).startsWith(query) }.thenBy { it.producer.lowercase() })
            .take(MAX)
    }
}
