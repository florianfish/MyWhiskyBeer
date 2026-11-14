package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelParserTest {

    private fun lines(vararg pairs: Pair<String, Int>) = pairs.map { OcrLine(it.first, it.second) }

    @Test
    fun scotchSingleMalt() {
        val guess = LabelParser.parse(
            lines(
                "ISLAY SINGLE MALT SCOTCH WHISKY" to 30,
                "LAGAVULIN" to 120,
                "AGED 16 YEARS" to 40,
                "Lagavulin Distillery, Isle of Islay" to 18,
                "43% vol" to 25,
                "70cl" to 25,
            ),
        )
        assertEquals(ProductType.WHISKY, guess.type)
        assertEquals("Lagavulin", guess.name)
        assertEquals(16, guess.age)
        assertEquals(43.0, guess.abv!!, 0.0)
        assertEquals("Single malt", guess.category)
        assertEquals("Islay", guess.region)
        assertEquals("Écosse", guess.country)
        assertTrue(guess.producer!!.startsWith("Lagavulin"))
    }

    @Test
    fun caskStrengthSherry() {
        val guess = LabelParser.parse(
            lines(
                "GLENFARCLAS" to 90,
                "105" to 100,
                "Cask Strength" to 30,
                "Matured in Oloroso sherry casks" to 20,
                "60% alc./vol." to 20,
            ),
        )
        assertEquals(ProductType.WHISKY, guess.type)
        assertTrue(guess.caskStrength)
        assertEquals("Sherry oloroso", guess.caskType)
        assertEquals(60.0, guess.abv!!, 0.0)
        // « 105 » n'a pas de lettres : le nom reste la marque
        assertEquals("Glenfarclas", guess.name)
    }

    @Test
    fun belgianBeer() {
        val guess = LabelParser.parse(
            lines(
                "BRASSERIE DE LA SENNE" to 25,
                "Zinnebir" to 110,
                "Blonde ale" to 30,
                "Bière belge · 5,8% vol" to 20,
                "33 cl" to 20,
            ),
        )
        assertEquals(ProductType.BEER, guess.type)
        assertEquals("Zinnebir", guess.name)
        assertEquals("De La Senne", guess.producer)
        assertEquals(5.8, guess.abv!!, 0.0)
        assertNull(guess.age)
        assertFalse(guess.caskStrength)
    }

    @Test
    fun neipaStyleWinsOverIpa() {
        val guess = LabelParser.parse(lines("Hazy Days" to 80, "New England IPA" to 30, "6.5%" to 20))
        assertEquals(ProductType.BEER, guess.type)
        assertEquals("NEIPA", guess.style)
        assertEquals(6.5, guess.abv!!, 0.0)
    }

    @Test
    fun emptyLabel() {
        val guess = LabelParser.parse(emptyList())
        assertNull(guess.type)
        assertNull(guess.name)
    }

    @Test
    fun prettify() {
        assertEquals("Ardbeg Uigeadail", LabelParser.prettify("ARDBEG  UIGEADAIL"))
        assertEquals("BrewDog", LabelParser.prettify("BrewDog"))
    }
}
