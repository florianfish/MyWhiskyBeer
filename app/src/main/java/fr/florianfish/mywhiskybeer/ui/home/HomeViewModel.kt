package fr.florianfish.mywhiskybeer.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.TastingWithProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    /** total = nombre de dégustations tous types confondus (carnet vide ou filtre sans résultat). */
    data class Ready(val latest: List<TastingWithProduct>, val total: Int) : HomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    tastingDao: TastingDao,
) : ViewModel() {

    /** null = bières et whiskies. Gardé en changeant d'onglet. */
    val filter: StateFlow<ProductType?> = savedStateHandle.getStateFlow<String?>(KEY_TYPE, null)
        .map { it?.let(ProductType::valueOf) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<HomeUiState> = combine(
        filter.flatMapLatest { type ->
            if (type == null) tastingDao.observeLatest(limit = LIMIT) else tastingDao.observeLatestByType(type, LIMIT)
        },
        tastingDao.observeCount(),
    ) { latest, total -> HomeUiState.Ready(latest, total) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun setFilter(type: ProductType?) {
        savedStateHandle[KEY_TYPE] = type?.name
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val LIMIT = 30
    }
}
