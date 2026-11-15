package fr.florianfish.mywhiskybeer.domain

import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.PeatLevel
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductFormTest {

    @Test
    fun whiskyToForm() {
        val form = ProductWithDetails(
            ProductEntity(id = 1, type = ProductType.WHISKY, name = "Lagavulin 16", producer = "Lagavulin", region = "Islay", abv = 43.0, barcode = "5000281005416"),
            beer = null,
            whisky = WhiskyDetailsEntity(productId = 1, category = "Single malt", age = 16, caskType = "Ex-bourbon", peat = PeatLevel.HEAVY),
        ).toForm()
        assertEquals(ProductType.WHISKY, form.type)
        assertEquals("Lagavulin 16", form.name)
        assertEquals("Islay", form.region)
        assertEquals("43", form.abv)
        assertEquals("5000281005416", form.barcode)
        assertEquals("16", form.age)
        assertEquals(PeatLevel.HEAVY, form.peat)
        assertEquals("", form.country)
    }

    @Test
    fun beerToForm() {
        val form = ProductWithDetails(
            ProductEntity(id = 2, type = ProductType.BEER, name = "Zinnebir", abv = 5.8),
            beer = BeerDetailsEntity(productId = 2, style = "Blonde", ibu = 30),
            whisky = null,
        ).toForm()
        assertEquals("5,8", form.abv)
        assertEquals("Blonde", form.style)
        assertEquals("30", form.ibu)
        assertEquals("", form.age)
    }
}
