package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingScaleTest {

    @Test
    fun beerStarsRoundTrip() {
        listOf(0.0, 0.5, 1.0, 2.5, 3.5, 4.5, 5.0).forEach { stars ->
            assertEquals(stars, RatingScale.toBeerStars(RatingScale.fromBeerStars(stars)), 0.0)
        }
    }

    @Test
    fun beerStarsRoundToHalf() {
        assertEquals(4.0, RatingScale.toBeerStars(82), 0.0)
        assertEquals(4.5, RatingScale.toBeerStars(87), 0.0)
    }

    @Test
    fun formatting() {
        assertEquals("4/5", RatingScale.format(80, ProductType.BEER))
        assertEquals("3,5/5", RatingScale.format(70, ProductType.BEER))
        assertEquals("87/100", RatingScale.format(87, ProductType.WHISKY))
    }

    @Test
    fun snapping() {
        assertEquals(70, RatingScale.snap(67f, ProductType.BEER))
        assertEquals(67, RatingScale.snap(67.4f, ProductType.WHISKY))
        assertEquals(100, RatingScale.snap(120f, ProductType.WHISKY))
        assertEquals(0, RatingScale.snap(-5f, ProductType.BEER))
    }
}
