package fr.florianfish.mywhiskybeer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.TastingWithProduct
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val latest: List<TastingWithProduct>) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    tastingDao: TastingDao,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = tastingDao.observeLatest(limit = 30)
        .map { HomeUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}
