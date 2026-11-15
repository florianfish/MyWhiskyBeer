package fr.florianfish.mywhiskybeer.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.domain.StatsCalculator
import fr.florianfish.mywhiskybeer.domain.StatsReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsState(val loading: Boolean = true, val report: StatsReport = StatsReport())

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    productDao: ProductDao,
    tastingDao: TastingDao,
) : ViewModel() {

    /** null = bières et whiskies ensemble. */
    val type: StateFlow<ProductType?> = savedStateHandle.getStateFlow<String?>(KEY_TYPE, null)
        .map { it?.let(ProductType::valueOf) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<StatsState> = combine(
        productDao.observeSummaries(),
        tastingDao.observeAll(),
        tastingDao.observeAllAromaIntensities(),
        type,
    ) { products, tastings, intensities, t ->
        StatsState(loading = false, report = StatsCalculator.compute(t, products, tastings, intensities))
    }
        .flowOn(Dispatchers.Default) // calculs hors du fil principal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    fun setType(value: ProductType?) {
        savedStateHandle[KEY_TYPE] = value?.name
    }

    private companion object {
        const val KEY_TYPE = "type"
    }
}
