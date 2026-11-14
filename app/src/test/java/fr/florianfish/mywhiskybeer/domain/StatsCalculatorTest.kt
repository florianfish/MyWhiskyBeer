package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.AromaIntensity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductSummary
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class StatsCalculatorTest {

    private val utc = ZoneOffset.UTC
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(utc).toInstant().toEpochMilli()

    private val tastings = listOf(
        TastingEntity(id = 1, productId = 1, rating = 92, date = day(2026, 9, 1)),
        TastingEntity(id = 2, productId = 1, rating = 88, date = day(2026, 10, 1)),
        TastingEntity(id = 3, productId = 2, rating = 80, date = day(2026, 10, 15)),
        TastingEntity(id = 4, productId = 3, rating = 70, date = day(2026, 11, 2)),
    )

    private fun summary(p: ProductEntity, beer: BeerDetailsEntity? = null, whisky: WhiskyDetailsEntity? = null): ProductSummary {
        val own = tastings.filter { it.productId == p.id }
        return ProductSummary(
            p, beer, whisky,
            avgRating = own.takeIf { it.isNotEmpty() }?.map { it.rating }?.average(),
            lastTasted = own.maxOfOrNull { it.date },
            tastingCount = own.size,
        )
    }

    private val products = listOf(
        summary(
            ProductEntity(id = 1, type = ProductType.WHISKY, name = "Lagavulin 16", producer = "Lagavulin", region = "Islay", country = "Écosse"),
            whisky = WhiskyDetailsEntity(productId = 1, caskType = "Ex-bourbon"),
        ),
        summary(
            ProductEntity(id = 2, type = ProductType.WHISKY, name = "Glenfarclas 105", producer = "Glenfarclas", region = "Speyside", country = "Ecosse"),
            whisky = WhiskyDetailsEntity(productId = 2, caskType = "Sherry"),
        ),
        summary(
            ProductEntity(id = 3, type = ProductType.BEER, name = "Zinnebir", producer = "De la Senne", country = "Belgique"),
            beer = BeerDetailsEntity(productId = 3, style = "Blonde"),
        ),
        summary(ProductEntity(id = 4, type = ProductType.BEER, name = "Jamais goûtée")),
    )

    private val intensities = listOf(
        AromaIntensity(1, 3, AromaEntity(1, ProductType.WHISKY, "Tourbé / fumé", "Tourbe")),
        AromaIntensity(3, 2, AromaEntity(2, ProductType.WHISKY, "Sherry", "Chocolat")),
        AromaIntensity(4, 1, AromaEntity(3, ProductType.BEER, "Malt", "Biscuit")),
    )

    @Test
    fun overall() {
        val r = StatsCalculator.compute(null, products, tastings, intensities, utc)
        assertEquals(4, r.tastingCount)
        assertEquals(4, r.productCount)
        assertEquals(83, r.avgRating) // (92 + 88 + 80 + 70) / 4 = 82,5
        assertEquals(listOf("Lagavulin 16", "Glenfarclas 105", "Zinnebir"), r.top.map { it.product.name })
        assertTrue(r.aromaProfile.isEmpty()) // pas de profil pour « Tout »
        // « Écosse » et « Ecosse » regroupés, sans accent pris en compte
        val pays = r.sections.first { it.title == "Pays préférés" }.groups
        assertEquals(3, pays.first { it.label.startsWith("É") || it.label.startsWith("E") }.tastings)
    }

    @Test
    fun whiskyOnly() {
        val r = StatsCalculator.compute(ProductType.WHISKY, products, tastings, intensities, utc)
        assertEquals(3, r.tastingCount)
        assertEquals(2, r.productCount)
        assertEquals(listOf("Régions préférées", "Distilleries préférées", "Fûts préférés"), r.sections.map { it.title })
        assertEquals(RatedGroup("Islay", 90, 2), r.sections[0].groups[0])
        // Profil : seules les familles whisky, la plus marquée vaut 1
        val profile = r.aromaProfile.toMap()
        assertEquals(1f, profile.getValue("Tourbé / fumé"), 0.001f)
        assertEquals(2f / 3f, profile.getValue("Sherry"), 0.001f)
    }

    @Test
    fun months() {
        val r = StatsCalculator.compute(null, products, tastings, intensities, utc)
        assertEquals(
            listOf(MonthPoint(YearMonth.of(2026, 9), 92, 1), MonthPoint(YearMonth.of(2026, 10), 84, 2), MonthPoint(YearMonth.of(2026, 11), 70, 1)),
            r.months,
        )
    }

    @Test
    fun beerWithoutTastingsIsEmpty() {
        val r = StatsCalculator.compute(ProductType.BEER, products.filter { it.product.id == 4L }, tastings, intensities, utc)
        assertEquals(0, r.tastingCount)
        assertEquals(1, r.productCount)
        assertTrue(r.top.isEmpty())
    }
}
