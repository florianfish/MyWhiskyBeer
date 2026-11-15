package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductSummary
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductListingTest {

    private fun whisky(id: Long, name: String, producer: String? = null, region: String? = null, cask: String? = null,
                       avg: Double? = null, last: Long? = null, created: Long = id) =
        ProductSummary(
            ProductEntity(id = id, type = ProductType.WHISKY, name = name, producer = producer, region = region, createdAt = created),
            beer = null,
            whisky = WhiskyDetailsEntity(productId = id, caskType = cask),
            avgRating = avg, lastTasted = last, tastingCount = if (avg == null) 0 else 1,
        )

    private fun beer(id: Long, name: String, style: String? = null, avg: Double? = null, last: Long? = null, created: Long = id) =
        ProductSummary(
            ProductEntity(id = id, type = ProductType.BEER, name = name, createdAt = created),
            beer = BeerDetailsEntity(productId = id, style = style),
            whisky = null,
            avgRating = avg, lastTasted = last, tastingCount = if (avg == null) 0 else 1,
        )

    private val all = listOf(
        whisky(1, "Lagavulin 16", producer = "Lagavulin", region = "Islay", avg = 92.0, last = 300, created = 10),
        whisky(2, "Glenfarclas 105", region = "Speyside", cask = "Sherry", avg = 85.0, last = 100, created = 30),
        beer(3, "Zinnebir", style = "Blonde", avg = 70.0, last = 200, created = 20),
        beer(4, "Ambrée de Bretagne", style = "Ambrée", created = 40),
        whisky(5, "Écosse mystère", created = 5),
    )

    private fun names(list: List<ProductSummary>) = list.map { it.product.name }

    @Test
    fun searchIgnoresAccentsAndCase() {
        assertEquals(listOf("Ambrée de Bretagne"), names(ProductListing.apply(all, "AMBREE", null, ProductSort.NAME)))
        assertEquals(listOf("Écosse mystère"), names(ProductListing.apply(all, "ecosse", null, ProductSort.NAME)))
    }

    @Test
    fun searchCoversProducerRegionStyleAndCask() {
        assertEquals(listOf("Lagavulin 16"), names(ProductListing.apply(all, "islay", null, ProductSort.NAME)))
        assertEquals(listOf("Zinnebir"), names(ProductListing.apply(all, "blonde", null, ProductSort.NAME)))
        assertEquals(listOf("Glenfarclas 105"), names(ProductListing.apply(all, "sherry", null, ProductSort.NAME)))
    }

    @Test
    fun allTermsMustMatch() {
        assertEquals(listOf("Lagavulin 16"), names(ProductListing.apply(all, "lagavulin islay", null, ProductSort.NAME)))
        assertEquals(emptyList<String>(), names(ProductListing.apply(all, "lagavulin speyside", null, ProductSort.NAME)))
    }

    @Test
    fun typeAndSearchCombine() {
        assertEquals(listOf("Zinnebir"), names(ProductListing.apply(all, "b", ProductType.BEER, ProductSort.NAME)).filter { it == "Zinnebir" })
        assertEquals(emptyList<String>(), names(ProductListing.apply(all, "islay", ProductType.BEER, ProductSort.NAME)))
        assertEquals(2, ProductListing.apply(all, "", ProductType.BEER, ProductSort.NAME).size)
    }

    @Test
    fun sortByNameIsAccentAware() {
        // « Écosse » se range avec les E, pas après le Z
        assertEquals(
            listOf("Ambrée de Bretagne", "Écosse mystère", "Glenfarclas 105", "Lagavulin 16", "Zinnebir"),
            names(ProductListing.apply(all, "", null, ProductSort.NAME)),
        )
    }

    @Test
    fun sortByRatingPutsUntastedLast() {
        assertEquals(
            listOf("Lagavulin 16", "Glenfarclas 105", "Zinnebir", "Ambrée de Bretagne", "Écosse mystère"),
            names(ProductListing.apply(all, "", null, ProductSort.RATING)),
        )
    }

    @Test
    fun sortByLastTastedAndAdded() {
        assertEquals(
            listOf("Lagavulin 16", "Zinnebir", "Glenfarclas 105", "Ambrée de Bretagne", "Écosse mystère"),
            names(ProductListing.apply(all, "", null, ProductSort.LAST_TASTED)),
        )
        assertEquals(
            listOf("Ambrée de Bretagne", "Glenfarclas 105", "Zinnebir", "Lagavulin 16", "Écosse mystère"),
            names(ProductListing.apply(all, "", null, ProductSort.ADDED)),
        )
    }

    @Test
    fun filterByStyleIgnoresVariants() {
        val list = listOf(beer(1, "A", "NEIPA"), beer(2, "B", "Hazy IPA"), beer(3, "C", "Stout"), whisky(4, "W"))
        assertEquals(listOf("A", "B"), names(ProductListing.apply(list, "", ProductType.BEER, ProductSort.NAME, style = "NEIPA")))
    }

    @Test
    fun sortByStyleGroupsInListOrder() {
        val list = listOf(
            beer(1, "Noire", "Stout", avg = 80.0),
            beer(2, "Sans style"),
            beer(3, "Trouble", "NEIPA", avg = 70.0),
            beer(4, "Simple", "IPA", avg = 60.0),
            beer(5, "Trouble 2", "Hazy IPA", avg = 90.0),
        )
        val sorted = ProductListing.apply(list, "", ProductType.BEER, ProductSort.STYLE)
        // IPA, puis NEIPA (meilleure note d'abord), Stout, et « non renseigné » à la fin
        assertEquals(listOf("Simple", "Trouble 2", "Trouble", "Noire", "Sans style"), names(sorted))
        assertEquals("Style non renseigné", ProductListing.groupKey(sorted.last()))
        assertEquals(listOf("IPA" to 1, "NEIPA" to 2, "Stout" to 1), ProductListing.beerStyles(list))
    }
}
