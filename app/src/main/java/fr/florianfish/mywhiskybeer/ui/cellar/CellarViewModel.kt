package fr.florianfish.mywhiskybeer.ui.cellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.CellarDao
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemWithProduct
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class CellarTab(val label: String, val statuses: Set<CellarStatus>) {
    STOCK("En stock", setOf(CellarStatus.IN_STOCK, CellarStatus.OPENED)),
    WISHLIST("À acheter", setOf(CellarStatus.WISHLIST)),
    EMPTY("Finies", setOf(CellarStatus.EMPTY)),
}

@HiltViewModel
class CellarViewModel @Inject constructor(cellarDao: CellarDao) : ViewModel() {

    val items: StateFlow<Map<CellarTab, List<CellarItemWithProduct>>> = cellarDao.observeAllWithProduct()
        .map { all ->
            val sorted = all.sortedWith(
                compareBy<CellarItemWithProduct> { it.item.status != CellarStatus.OPENED } // entamées d'abord
                    .thenBy { it.product.name.lowercase() },
            )
            CellarTab.entries.associateWith { tab -> sorted.filter { it.item.status in tab.statuses } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
