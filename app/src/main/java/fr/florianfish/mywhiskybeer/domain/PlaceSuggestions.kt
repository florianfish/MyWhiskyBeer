package fr.florianfish.mywhiskybeer.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** Suggestions pour le champ « Lieu » : lieux déjà utilisés (hors-ligne) puis villes trouvées en ligne. */
object PlaceSuggestions {

    const val MAX = 5

    /** Nombre de lettres avant d'interroger le service de villes. */
    const val ONLINE_MIN_LENGTH = 3

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Villes d'une réponse Photon (GeoJSON) : « Rennes, France », « Bowmore, Royaume-Uni ».
     * Liste vide si la réponse est illisible.
     */
    fun parsePhoton(body: String): List<String> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
        val features = root["features"] as? JsonArray ?: return emptyList()
        return features.mapNotNull { feature ->
            val props = (feature as? JsonObject)?.get("properties") as? JsonObject ?: return@mapNotNull null
            val name = props.string("name") ?: return@mapNotNull null
            listOfNotNull(name, props.string("country")).joinToString(", ")
        }.distinctBy { LabelParser.fold(it) }
    }

    /** Lieux déjà utilisés qui contiennent le texte saisi (sans accents ni casse), du plus récent au plus ancien. */
    fun matchPast(pastPlaces: List<String>, typed: String): List<String> {
        val query = LabelParser.fold(typed).trim()
        if (query.isEmpty()) return emptyList()
        return pastPlaces
            .distinctBy { LabelParser.fold(it).trim() }
            .filter { query in LabelParser.fold(it) }
            .sortedBy { !LabelParser.fold(it).startsWith(query) } // tri stable : les plus récents restent devant
    }

    /**
     * Fusion : lieux déjà utilisés d'abord, puis villes en ligne, sans doublons, au plus [MAX].
     * Rien si le texte saisi est déjà exactement l'une des suggestions.
     */
    fun merge(typed: String, past: List<String>, online: List<String>): List<String> {
        val query = LabelParser.fold(typed).trim()
        val all = (past + online).distinctBy { LabelParser.fold(it).trim() }
        if (all.any { LabelParser.fold(it).trim() == query }) return emptyList()
        return all.take(MAX)
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
}
