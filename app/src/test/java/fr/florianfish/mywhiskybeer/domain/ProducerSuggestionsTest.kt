package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProducerInfo
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProducerSuggestionsTest {

    // Du plus récent au plus ancien, comme la requête
    private val known = listOf(
        ProducerInfo("Lagavulin", ProductType.WHISKY, null, null),
        ProducerInfo("Brasserie de la Senne", ProductType.BEER, "Belgique", null),
        ProducerInfo("Laphroaig", ProductType.WHISKY, "Écosse", "Islay"),
        ProducerInfo("Lagavulin", ProductType.WHISKY, "Écosse", "Islay"),
        ProducerInfo("Glenfarclas", ProductType.WHISKY, "Écosse", "Speyside"),
        ProducerInfo("Saint-Germain", ProductType.BEER, "France", "Hauts-de-France"),
    )

    private fun names(r: List<ProducerInfo>) = r.map { it.producer }

    @Test
    fun prefixFirstThenContains() {
        // « Glenfarclas » contient « la » : proposé, mais après ceux qui commencent par « la »
        assertEquals(listOf("Lagavulin", "Laphroaig", "Glenfarclas"), names(ProducerSuggestions.match(known, ProductType.WHISKY, "la")))
        assertEquals(listOf("Glenfarclas"), names(ProducerSuggestions.match(known, ProductType.WHISKY, "farc")))
    }

    @Test
    fun ignoresAccentsAndCase() {
        assertEquals(listOf("Brasserie de la Senne"), names(ProducerSuggestions.match(known, ProductType.BEER, "SENNE")))
        // Accent tapé en trop : reconnu quand même
        assertEquals(listOf("Saint-Germain"), names(ProducerSuggestions.match(known, ProductType.BEER, "germàin")))
    }

    @Test
    fun onlySameType() {
        assertTrue(ProducerSuggestions.match(known, ProductType.BEER, "lag").isEmpty())
    }

    @Test
    fun nothingForShortOrExactInput() {
        assertTrue(ProducerSuggestions.match(known, ProductType.WHISKY, "l").isEmpty())
        assertTrue(ProducerSuggestions.match(known, ProductType.WHISKY, "lagavulin").isEmpty())
    }

    @Test
    fun duplicatesMergedWithKnownOrigin() {
        val lagavulin = ProducerSuggestions.match(known, ProductType.WHISKY, "lagav").single()
        // Le produit le plus récent n'a pas d'origine : on reprend celle d'un produit plus ancien
        assertEquals("Écosse", lagavulin.country)
        assertEquals("Islay", lagavulin.region)
    }

    @Test
    fun withProducerKeepsTypedOrigin() {
        val info = ProducerInfo("Lagavulin", ProductType.WHISKY, "Écosse", "Islay")
        val form = ProductForm(producer = "lagav", region = "Islay (Port Ellen)").withProducer(info)
        assertEquals("Lagavulin", form.producer)
        assertEquals("Écosse", form.country)
        assertEquals("Islay (Port Ellen)", form.region)
    }
}
