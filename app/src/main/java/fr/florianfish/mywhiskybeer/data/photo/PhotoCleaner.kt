package fr.florianfish.mywhiskybeer.data.photo

import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import javax.inject.Inject

/**
 * Supprime une photo remplacée, sauf si elle sert encore : un produit et sa première
 * dégustation partagent souvent le même fichier.
 */
class PhotoCleaner @Inject constructor(
    private val productDao: ProductDao,
    private val tastingDao: TastingDao,
    private val photoStore: PhotoStore,
) {
    suspend fun deleteIfUnused(path: String?) {
        if (path == null) return
        if (productDao.countPhotoUses(path) == 0 && tastingDao.countPhotoUses(path) == 0) {
            photoStore.delete(path)
        }
    }
}
