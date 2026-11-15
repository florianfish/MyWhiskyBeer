package fr.florianfish.mywhiskybeer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSuggestionsTest {

    @Test
    fun parsePhoton() {
        val body = """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{"name":"Rennes","state":"Bretagne","country":"France","countrycode":"FR","osm_key":"place","osm_value":"city"},"geometry":{"type":"Point","coordinates":[-1.68,48.11]}},
              {"type":"Feature","properties":{"name":"Rennes","country":"France"}},
              {"type":"Feature","properties":{"name":"Bowmore","country":"Royaume-Uni"}},
              {"type":"Feature","properties":{"country":"Sans nom"}}
            ]}
        """.trimIndent()
        assertEquals(listOf("Rennes, France", "Bowmore, Royaume-Uni"), PlaceSuggestions.parsePhoton(body))
        assertTrue(PlaceSuggestions.parsePhoton("erreur").isEmpty())
    }

    @Test
    fun matchPastIgnoresAccentsAndPutsPrefixFirst() {
        val past = listOf("Le Comptoir, Rennes", "Rennes, France", "Brasserie de l'Écluse", "rennes, france")
        assertEquals(listOf("Rennes, France", "Le Comptoir, Rennes"), PlaceSuggestions.matchPast(past, "renn"))
        assertEquals(listOf("Brasserie de l'Écluse"), PlaceSuggestions.matchPast(past, "ecluse"))
        assertTrue(PlaceSuggestions.matchPast(past, " ").isEmpty())
    }

    @Test
    fun mergePastFirstWithoutDuplicates() {
        val merged = PlaceSuggestions.merge(
            "renn",
            past = listOf("Rennes, France", "Le Comptoir, Rennes"),
            online = listOf("Rennes, France", "Rennes-le-Château, France"),
        )
        assertEquals(listOf("Rennes, France", "Le Comptoir, Rennes", "Rennes-le-Château, France"), merged)
    }

    @Test
    fun nothingWhenAlreadyChosen() {
        assertTrue(PlaceSuggestions.merge("Rennes, France", listOf("Rennes, France"), emptyList()).isEmpty())
    }
}
