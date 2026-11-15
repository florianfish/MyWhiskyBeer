package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.AromaSeed
import fr.florianfish.mywhiskybeer.data.local.entity.AromaIntensity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType

/**
 * Profil aromatique : somme des intensités par famille, ramenée à 0..1
 * par rapport à la famille la plus marquée.
 */
fun aromaProfile(type: ProductType, intensities: List<AromaIntensity>): List<Pair<String, Float>> {
    if (intensities.isEmpty()) return emptyList()
    val families = when (type) {
        ProductType.WHISKY -> AromaSeed.whisky.keys
        ProductType.BEER -> AromaSeed.beer.keys
    }
    val totals = intensities.groupBy { it.aroma.family }.mapValues { (_, list) -> list.sumOf { it.intensity } }
    val max = totals.values.maxOrNull()?.takeIf { it > 0 } ?: return emptyList()
    return families.map { it to (totals[it] ?: 0).toFloat() / max }
}
