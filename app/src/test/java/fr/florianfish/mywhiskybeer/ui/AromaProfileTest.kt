package fr.florianfish.mywhiskybeer.ui

import fr.florianfish.mywhiskybeer.data.local.AromaSeed
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.AromaIntensity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.domain.aromaProfile
import fr.florianfish.mywhiskybeer.domain.toDecimalOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AromaProfileTest {

    private fun aroma(family: String, intensity: Int, tastingId: Long = 1) =
        AromaIntensity(tastingId, intensity, AromaEntity(0, ProductType.WHISKY, family, "x"))

    @Test
    fun emptyWhenNoAroma() {
        assertTrue(aromaProfile(ProductType.WHISKY, emptyList()).isEmpty())
    }

    @Test
    fun normalizedOnStrongestFamily() {
        val profile = aromaProfile(
            ProductType.WHISKY,
            listOf(aroma("Fruité", 3), aroma("Fruité", 1), aroma("Boisé", 2)),
        ).toMap()

        assertEquals(AromaSeed.whisky.size, profile.size)
        assertEquals(1f, profile.getValue("Fruité"), 0.001f)
        assertEquals(0.5f, profile.getValue("Boisé"), 0.001f)
        assertEquals(0f, profile.getValue("Marin"), 0.001f)
    }

    @Test
    fun decimalAcceptsFrenchComma() {
        assertEquals(46.3, " 46,3 ".toDecimalOrNull()!!, 0.0)
        assertEquals(5.0, "5".toDecimalOrNull()!!, 0.0)
        assertNull("abc".toDecimalOrNull())
    }
}
