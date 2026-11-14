package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsParserTest {

    @Test
    fun beer() {
        val guess = OpenFoodFactsParser.parse(
            """
            {"code":"5410908000012","status":1,"status_verbose":"product found","product":{
              "product_name":"Zinnebir","brands":"Brasserie de la Senne, Senne",
              "nutriments":{"alcohol_100g":5.8,"alcohol_unit":"% vol"},
              "categories_tags":["en:beverages","en:alcoholic-beverages","en:beers","en:blonde-beers"],
              "countries_tags":["en:belgium"]}}
            """.trimIndent(),
        )!!
        assertEquals(ProductType.BEER, guess.type)
        assertEquals("Zinnebir", guess.name)
        assertEquals("Brasserie de la Senne", guess.producer)
        assertEquals(5.8, guess.abv!!, 0.0)
        assertEquals("Belgique", guess.country)
        assertNull(guess.age)
    }

    @Test
    fun whiskyWithFrenchNameAndStringAlcohol() {
        val guess = OpenFoodFactsParser.parse(
            """
            {"status":1,"product":{
              "product_name":"Lagavulin 16 Years","product_name_fr":"Lagavulin 16 ans",
              "brands":"LAGAVULIN","origins":"Islay, Écosse",
              "nutriments":{"alcohol":"43"},
              "categories_tags":["en:alcoholic-beverages","en:whiskies","en:scotch-whiskies"]}}
            """.trimIndent(),
        )!!
        assertEquals(ProductType.WHISKY, guess.type)
        assertEquals("Lagavulin 16 ans", guess.name)
        assertEquals("Lagavulin", guess.producer)
        assertEquals(43.0, guess.abv!!, 0.0)
        assertEquals(16, guess.age)
        assertEquals("Islay", guess.region)
    }

    @Test
    fun unknownProduct() {
        assertNull(OpenFoodFactsParser.parse("""{"code":"123","status":0,"status_verbose":"product not found"}"""))
        assertNull(OpenFoodFactsParser.parse("pas du json"))
    }

    @Test
    fun unrealisticAlcoholIsIgnored() {
        val guess = OpenFoodFactsParser.parse("""{"status":1,"product":{"product_name":"Sirop","nutriments":{"alcohol_100g":0}}}""")!!
        assertNull(guess.abv)
    }

    @Test
    fun barcodeChecksum() {
        assertTrue(Barcodes.isValid("3017624010701")) // EAN-13
        assertTrue(Barcodes.isValid("96385074"))      // EAN-8
        assertTrue(Barcodes.isValid("036000291452"))  // UPC-A
        assertFalse(Barcodes.isValid("3017624010702"))
        assertFalse(Barcodes.isValid("ABC"))
    }
}
