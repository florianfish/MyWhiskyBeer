package fr.florianfish.mywhiskybeer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeerStylesTest {

    @Test
    fun knownStylesIgnoreCaseAndAccents() {
        assertEquals("NEIPA", BeerStyles.canonical("neipa"))
        assertEquals("Ambrée", BeerStyles.canonical("AMBREE"))
        assertEquals("Kölsch", BeerStyles.canonical("kolsch"))
        assertEquals("Imperial stout", BeerStyles.canonical("  Imperial   Stout "))
    }

    @Test
    fun aliases() {
        assertEquals("NEIPA", BeerStyles.canonical("Hazy IPA"))
        assertEquals("NEIPA", BeerStyles.canonical("New England IPA"))
        assertEquals("Triple", BeerStyles.canonical("Tripel"))
        assertEquals("Blanche", BeerStyles.canonical("Witbier"))
        assertEquals("Pils", BeerStyles.canonical("Pilsner"))
        assertEquals("Double IPA", BeerStyles.canonical("DIPA"))
    }

    @Test
    fun unknownKeptAndEmptyIsNull() {
        assertEquals("Grisette", BeerStyles.canonical(" Grisette "))
        assertNull(BeerStyles.canonical("  "))
        assertNull(BeerStyles.canonical(null))
    }

    @Test
    fun everyParserStyleIsInTheList() {
        // Les styles devinés depuis l'étiquette doivent tomber dans la liste
        listOf("NEIPA", "Hazy IPA", "West Coast IPA", "Double IPA", "Session IPA", "Imperial stout", "Pale ale",
            "Brown ale", "Red ale", "Barley wine", "Berliner weisse", "IPA", "Stout", "Porter", "Pils", "Lager",
            "Saison", "Triple", "Quadruple", "Double", "Blanche", "Weizen", "Gose", "Sour", "Bock", "Kölsch", "Ambrée",
        ).forEach { assertTrue(it, BeerStyles.canonical(it) in BeerStyles.ALL) }
        assertTrue(BeerStyles.POPULAR.all { it in BeerStyles.ALL })
    }

    @Test
    fun order() {
        assertTrue(BeerStyles.order("IPA") < BeerStyles.order("Stout"))
        assertEquals(BeerStyles.ALL.size, BeerStyles.order("Grisette"))
    }
}
