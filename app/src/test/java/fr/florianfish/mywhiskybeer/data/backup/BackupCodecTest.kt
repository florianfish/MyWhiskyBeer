package fr.florianfish.mywhiskybeer.data.backup

import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import fr.florianfish.mywhiskybeer.data.local.entity.PeatLevel
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val sample = BackupData(
        createdAt = 1_000L,
        appVersion = "1.0.0",
        products = listOf(
            ProductEntity(id = 1, type = ProductType.WHISKY, name = "Lagavulin 16", abv = 43.0, tags = listOf("islay", "tourbé"), photoPath = "/data/x/photos/a.jpg"),
            ProductEntity(id = 2, type = ProductType.BEER, name = "Zinnebir"),
        ),
        beerDetails = listOf(BeerDetailsEntity(productId = 2, style = "Blonde")),
        whiskyDetails = listOf(WhiskyDetailsEntity(productId = 1, age = 16, peat = PeatLevel.HEAVY)),
        tastings = listOf(
            TastingEntity(id = 10, productId = 1, rating = 92, nose = "Fumée", photoPath = "/data/x/photos/a.jpg"),
            TastingEntity(id = 11, productId = 2, rating = 80, photoPath = "/data/x/photos/b.jpg"),
        ),
        aromas = listOf(AromaRef(10, ProductType.WHISKY, "Tourbé / fumé", "Tourbe", 3)),
        cellar = listOf(
            CellarItemEntity(productId = 1, status = CellarStatus.OPENED, fillLevel = 40),
            CellarItemEntity(productId = 2, status = CellarStatus.WISHLIST),
        ),
    )

    @Test
    fun roundTrip() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample))
        assertEquals(sample, decoded)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val json = BackupCodec.encode(sample).replaceFirst("{", """{"futureField":42,""")
        assertEquals(sample, BackupCodec.decode(json))
    }

    @Test(expected = IllegalArgumentException::class)
    fun newerFormatIsRefused() {
        BackupCodec.decode(BackupCodec.encode(sample.copy(format = BackupData.FORMAT + 1)))
    }

    @Test
    fun photosBecomeFileNames() {
        val portable = BackupCodec.mapPhotos(sample) { it.substringAfterLast('/') }
        assertEquals("a.jpg", portable.products[0].photoPath)
        assertNull(portable.products[1].photoPath)
        assertEquals("b.jpg", portable.tastings[1].photoPath)
    }

    @Test
    fun summary() {
        val s = BackupCodec.summary(sample)
        assertEquals(2, s.products)
        assertEquals(2, s.tastings)
        assertEquals(2, s.bottles) // la bouteille entamée × 2 ; la liste d'achats ne compte pas
        assertEquals(2, s.photos)
        assertEquals("2 produits, 2 dégustations, 2 bouteilles, 2 photos", s.text)
    }

    @Test
    fun unsafeNamesAreRejected() {
        assertTrue(BackupCodec.isSafePhotoName("3f2a-11.jpg"))
        assertFalse(BackupCodec.isSafePhotoName("../evil.jpg"))
        assertFalse(BackupCodec.isSafePhotoName("dir/a.jpg"))
        assertFalse(BackupCodec.isSafePhotoName(".."))
    }
}
