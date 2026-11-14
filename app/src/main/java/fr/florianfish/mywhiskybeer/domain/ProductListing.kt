package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.ProductSummary
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import java.text.Collator
import java.util.Locale

/** Ordres de tri du carnet. */
enum class ProductSort(val label: String) {
    NAME("Nom"),
    RATING("Meilleure note"),
    LAST_TASTED("Dernière dégustation"),
    ADDED("Ajout récent"),
}

/**
 * Recherche et tri du carnet, en mémoire : la recherche ignore accents et casse
 * (ce que LIKE de SQLite ne sait pas faire) et porte sur tous les champs utiles.
 */
object ProductListing {

    private val collator: Collator = Collator.getInstance(Locale.FRENCH).apply { strength = Collator.PRIMARY }

    fun apply(
        products: List<ProductSummary>,
        query: String,
        type: ProductType?,
        sort: ProductSort,
    ): List<ProductSummary> {
        val terms = LabelParser.fold(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        val filtered = products.filter { p ->
            (type == null || p.product.type == type) &&
                (terms.isEmpty() || searchText(p).let { text -> terms.all { it in text } })
        }
        return filtered.sortedWith(comparator(sort))
    }

    /** Tout ce sur quoi on peut chercher, en minuscules sans accents. */
    internal fun searchText(p: ProductSummary): String = LabelParser.fold(
        listOfNotNull(
            p.product.name,
            p.product.producer,
            p.product.region,
            p.product.country,
            p.beer?.style,
            p.beer?.color,
            p.whisky?.category,
            p.whisky?.caskType,
            p.whisky?.age?.let { "$it ans" },
        ).plus(p.product.tags).joinToString(" "),
    )

    private fun comparator(sort: ProductSort): Comparator<ProductSummary> {
        val byName = Comparator<ProductSummary> { a, b -> collator.compare(a.product.name, b.product.name) }
        return when (sort) {
            ProductSort.NAME -> byName
            // Sans dégustation : en fin de liste
            ProductSort.RATING -> compareByDescending<ProductSummary, Double?>(nullsFirst()) { it.avgRating }.then(byName)
            ProductSort.LAST_TASTED -> compareByDescending<ProductSummary, Long?>(nullsFirst()) { it.lastTasted }.then(byName)
            ProductSort.ADDED -> compareByDescending<ProductSummary> { it.product.createdAt }.then(byName)
        }
    }
}
