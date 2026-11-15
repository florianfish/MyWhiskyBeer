package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.AromaIntensity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductSummary
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

/** Un groupe (région, style, producteur…) avec sa note moyenne sur 100 et son nombre de dégustations. */
data class RatedGroup(val label: String, val avgRating: Int, val tastings: Int)

/** Section de groupes préférés, par exemple « Régions préférées ». */
data class GroupSection(val title: String, val groups: List<RatedGroup>)

/** Note moyenne d'un mois. */
data class MonthPoint(val month: YearMonth, val avgRating: Int, val tastings: Int)

data class StatsReport(
    val tastingCount: Int = 0,
    val productCount: Int = 0,
    val countryCount: Int = 0,
    /** Note moyenne de toutes les dégustations, sur 100. */
    val avgRating: Int? = null,
    /** Les 10 produits les mieux notés (moyenne de leurs dégustations). */
    val top: List<ProductSummary> = emptyList(),
    val sections: List<GroupSection> = emptyList(),
    /** Moyenne par mois, 12 derniers mois ayant des dégustations, du plus ancien au plus récent. */
    val months: List<MonthPoint> = emptyList(),
    /** Pays → nombre de produits goûtés, du plus fréquent au moins fréquent. */
    val countries: List<Pair<String, Int>> = emptyList(),
    /** Profil aromatique global (vide pour « Tout » : les familles diffèrent entre bière et whisky). */
    val aromaProfile: List<Pair<String, Float>> = emptyList(),
)

object StatsCalculator {

    const val TOP_SIZE = 10
    const val GROUP_SIZE = 5
    const val MONTHS = 12

    fun compute(
        type: ProductType?,
        products: List<ProductSummary>,
        tastings: List<TastingEntity>,
        intensities: List<AromaIntensity>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): StatsReport {
        val scoped = products.filter { type == null || it.product.type == type }
        val ids = scoped.map { it.product.id }.toSet()
        val scopedTastings = tastings.filter { it.productId in ids }
        if (scopedTastings.isEmpty()) return StatsReport(productCount = scoped.size)
        val tasted = scoped.filter { it.avgRating != null && it.tastingCount > 0 }

        val sections = when (type) {
            ProductType.WHISKY -> listOf(
                section("Régions préférées", tasted) { it.product.region },
                section("Distilleries préférées", tasted) { it.product.producer },
                section("Fûts préférés", tasted) { it.whisky?.caskType },
            )
            ProductType.BEER -> listOf(
                section("Styles préférés", tasted) { it.beer?.style },
                section("Brasseries préférées", tasted) { it.product.producer },
            )
            null -> listOf(
                section("Pays préférés", tasted) { it.product.country },
                section("Producteurs préférés", tasted) { it.product.producer },
            )
        }.filter { it.groups.isNotEmpty() }

        val months = scopedTastings
            .groupBy { YearMonth.from(Instant.ofEpochMilli(it.date).atZone(zone)) }
            .map { (month, list) -> MonthPoint(month, list.map { it.rating }.average().roundToInt(), list.size) }
            .sortedBy { it.month }
            .takeLast(MONTHS)

        val countries = tasted
            .mapNotNull { it.product.country?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }.eachCount()
            .toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

        val profile = if (type == null) {
            emptyList()
        } else {
            val tastingIds = scopedTastings.map { it.id }.toSet()
            aromaProfile(type, intensities.filter { it.tastingId in tastingIds && it.aroma.productType == type })
        }

        return StatsReport(
            tastingCount = scopedTastings.size,
            productCount = scoped.size,
            countryCount = countries.size,
            avgRating = scopedTastings.map { it.rating }.average().roundToInt(),
            top = tasted
                .sortedWith(compareByDescending<ProductSummary> { it.avgRating }.thenByDescending { it.tastingCount })
                .take(TOP_SIZE),
            sections = sections,
            months = months,
            countries = countries,
            aromaProfile = profile,
        )
    }

    /**
     * Groupes classés par note moyenne (pondérée par le nombre de dégustations),
     * puis par nombre de dégustations. Groupes sans libellé ignorés.
     */
    private fun section(title: String, products: List<ProductSummary>, key: (ProductSummary) -> String?): GroupSection {
        val groups = products
            .mapNotNull { p -> key(p)?.trim()?.takeIf(String::isNotEmpty)?.let { it to p } }
            .groupBy({ LabelParser.fold(it.first) }) { it }
            .map { (_, entries) ->
                val label = entries.first().first
                val count = entries.sumOf { it.second.tastingCount }
                val weighted = entries.sumOf { (it.second.avgRating ?: 0.0) * it.second.tastingCount } / count
                RatedGroup(label, weighted.roundToInt(), count)
            }
            .sortedWith(compareByDescending<RatedGroup> { it.avgRating }.thenByDescending { it.tastings }.thenBy { it.label })
            .take(GROUP_SIZE)
        return GroupSection(title, groups)
    }
}
