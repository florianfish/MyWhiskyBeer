package fr.florianfish.mywhiskybeer.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productDao: ProductDao,
) : ViewModel() {

    /** null = tous les types. */
    private val _filter = MutableStateFlow<ProductType?>(null)
    val filter: StateFlow<ProductType?> = _filter.asStateFlow()

    val products: StateFlow<List<ProductWithDetails>> = _filter
        .flatMapLatest { type ->
            if (type == null) productDao.observeAll() else productDao.observeByType(type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(type: ProductType?) {
        _filter.value = type
    }
}
