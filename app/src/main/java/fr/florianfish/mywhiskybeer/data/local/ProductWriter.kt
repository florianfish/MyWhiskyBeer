package fr.florianfish.mywhiskybeer.data.local

import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import fr.florianfish.mywhiskybeer.domain.ProductForm
import fr.florianfish.mywhiskybeer.domain.blankToNull
import fr.florianfish.mywhiskybeer.domain.toDecimalOrNull
import javax.inject.Inject

/** Crée un produit (tronc commun + détails du type) à partir du formulaire. */
class ProductWriter @Inject constructor(
    private val productDao: ProductDao,
) {
    suspend fun insert(form: ProductForm, photoPath: String?): Long {
        val product = ProductEntity(
            type = form.type,
            name = form.name.trim(),
            producer = form.producer.blankToNull(),
            country = form.country.blankToNull(),
            region = form.region.blankToNull(),
            abv = form.abv.toDecimalOrNull(),
            photoPath = photoPath,
        )
        return when (form.type) {
            ProductType.BEER -> productDao.insertBeer(
                product,
                BeerDetailsEntity(
                    productId = 0,
                    style = form.style.blankToNull(),
                    ibu = form.ibu.trim().toIntOrNull(),
                    color = form.color.blankToNull(),
                ),
            )
            ProductType.WHISKY -> productDao.insertWhisky(
                product,
                WhiskyDetailsEntity(
                    productId = 0,
                    category = form.category.blankToNull(),
                    age = form.age.trim().toIntOrNull(),
                    caskType = form.caskType.blankToNull(),
                    peat = form.peat,
                    caskStrength = form.caskStrength,
                ),
            )
        }
    }

    /** Donne une photo à un produit qui n'en a pas encore. */
    suspend fun setPhotoIfMissing(product: ProductEntity, photoPath: String?) {
        if (product.photoPath == null && photoPath != null) {
            productDao.updateProduct(product.copy(photoPath = photoPath))
        }
    }
}
