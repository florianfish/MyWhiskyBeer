package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import kotlin.math.roundToInt

/**
 * Les notes sont stockées sur 100 et affichées selon le type :
 * bière sur 5 (demi-points), whisky sur 100.
 */
object RatingScale {

    fun format(rating: Int, type: ProductType): String = when (type) {
        ProductType.BEER -> {
            val stars = toBeerStars(rating)
            val text = if (stars % 1.0 == 0.0) stars.toInt().toString() else stars.toString().replace('.', ',')
            "$text/5"
        }
        ProductType.WHISKY -> "$rating/100"
    }

    /** 0..100 → 0..5 par pas de 0,5. */
    fun toBeerStars(rating: Int): Double = (rating.coerceIn(0, 100) / 10.0).roundToInt() / 2.0

    /** 0..5 (pas de 0,5) → 0..100. */
    fun fromBeerStars(stars: Double): Int = (stars.coerceIn(0.0, 5.0) * 20).roundToInt()
}
