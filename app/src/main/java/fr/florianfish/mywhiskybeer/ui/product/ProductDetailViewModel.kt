package fr.florianfish.mywhiskybeer.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.CellarDao
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.local.entity.TastingWithProduct
import fr.florianfish.mywhiskybeer.data.photo.PhotoStore
import fr.florianfish.mywhiskybeer.domain.aromaProfile
import fr.florianfish.mywhiskybeer.ui.navigation.ProductDetailRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailState(
    val loading: Boolean = true,
    val product: ProductWithDetails? = null,
    val tastings: List<TastingWithProduct> = emptyList(),
    val cellar: CellarItemEntity? = null,
    /** Famille → valeur 0..1, vide s'il n'y a aucun arôme noté. */
    val profile: List<Pair<String, Float>> = emptyList(),
) {
    val averageRating: Int? get() = tastings.takeIf { it.isNotEmpty() }?.map { it.tasting.rating }?.average()?.toInt()
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productDao: ProductDao,
    private val tastingDao: TastingDao,
    private val cellarDao: CellarDao,
    private val photoStore: PhotoStore,
) : ViewModel() {

    private val productId = savedStateHandle.toRoute<ProductDetailRoute>().productId

    val state: StateFlow<ProductDetailState> = combine(
        productDao.observeById(productId),
        tastingDao.observeForProduct(productId),
        cellarDao.observeForProduct(productId),
        tastingDao.observeAromaIntensities(productId),
    ) { product, tastings, cellar, intensities ->
        ProductDetailState(
            loading = false,
            product = product,
            tastings = tastings,
            cellar = cellar,
            profile = product?.let { aromaProfile(it.product.type, intensities) }.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailState())

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    fun setCellarStatus(status: CellarStatus?) {
        viewModelScope.launch {
            val current = state.value.cellar
            if (status == null) {
                cellarDao.deleteForProduct(productId)
            } else {
                cellarDao.upsert(
                    (current ?: CellarItemEntity(productId = productId, status = status)).copy(
                        status = status,
                        fillLevel = when (status) {
                            CellarStatus.IN_STOCK -> 100
                            CellarStatus.EMPTY -> 0
                            CellarStatus.OPENED -> current?.fillLevel?.takeIf { it in 1..99 } ?: 75
                            CellarStatus.WISHLIST -> null
                        },
                    ),
                )
            }
        }
    }

    fun setFillLevel(level: Int) {
        val current = state.value.cellar ?: return
        viewModelScope.launch { cellarDao.upsert(current.copy(fillLevel = level.coerceIn(0, 100))) }
    }

    fun deleteTasting(item: TastingWithProduct) {
        viewModelScope.launch {
            tastingDao.deleteTasting(item.tasting)
            // Photo propre à la dégustation (pas celle du produit)
            if (item.tasting.photoPath != item.product.photoPath) photoStore.delete(item.tasting.photoPath)
        }
    }

    fun deleteProduct() {
        val s = state.value
        val product = s.product?.product ?: return
        viewModelScope.launch {
            val photos = (s.tastings.map { it.tasting.photoPath } + product.photoPath).filterNotNull().toSet()
            productDao.deleteProduct(product) // dégustations, détails et cave supprimés en cascade
            photos.forEach { photoStore.delete(it) }
            _deleted.send(Unit)
        }
    }
}
