package fr.florianfish.mywhiskybeer.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.entity.ProductSummary
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.settings.AppSettings
import fr.florianfish.mywhiskybeer.domain.ProductListing
import fr.florianfish.mywhiskybeer.domain.ProductSort
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProductsState(
    val products: List<ProductSummary> = emptyList(),
    /** Nombre total de produits du carnet (pour distinguer « carnet vide » et « aucun résultat »). */
    val total: Int = 0,
    val loading: Boolean = true,
)

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    productDao: ProductDao,
    private val settings: AppSettings,
) : ViewModel() {

    // Recherche et filtre gardés en changeant d'onglet ; le tri est gardé même après redémarrage
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    val filter: StateFlow<ProductType?> = savedStateHandle.getStateFlow<String?>(KEY_TYPE, null)
        .map { name -> name?.let(ProductType::valueOf) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val sort: StateFlow<ProductSort> = settings.productSort

    val state: StateFlow<ProductsState> = combine(productDao.observeSummaries(), query, filter, sort) { all, q, type, s ->
        ProductsState(products = ProductListing.apply(all, q, type, s), total = all.size, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductsState())

    fun setQuery(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun setFilter(type: ProductType?) {
        savedStateHandle[KEY_TYPE] = type?.name
    }

    fun setSort(value: ProductSort) = settings.setProductSort(value)

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_TYPE = "type"
    }
}
