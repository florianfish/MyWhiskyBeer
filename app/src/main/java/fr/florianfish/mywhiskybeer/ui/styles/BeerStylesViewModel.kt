package fr.florianfish.mywhiskybeer.ui.styles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.dao.BeerStyleDao
import fr.florianfish.mywhiskybeer.domain.BeerStyles
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Un style de la liste et le nombre de bières qui l'utilisent. */
data class StyleRow(val name: String, val count: Int)

data class BeerStylesState(
    val styles: List<StyleRow> = emptyList(),
    /** Styles de la liste par défaut qui ont été retirés, à rajouter d'un appui. */
    val removedDefaults: List<String> = emptyList(),
)

/** Liste des styles de bière : gestion (Réglages) et choix dans le formulaire produit. */
@HiltViewModel
class BeerStylesViewModel @Inject constructor(
    private val dao: BeerStyleDao,
) : ViewModel() {

    val state: StateFlow<BeerStylesState> = combine(dao.observeAll(), dao.observeUsage()) { all, usage ->
        val counts = usage.associate { it.style.lowercase() to it.count }
        val names = all.map { it.name.lowercase() }.toSet()
        BeerStylesState(
            styles = all.map { StyleRow(it.name, counts[it.name.lowercase()] ?: 0) },
            removedDefaults = BeerStyles.ALL.filter { it.lowercase() !in names },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BeerStylesState())

    /** Noms seuls, pour le sélecteur. */
    val names: StateFlow<List<String>> = state.map { s -> s.styles.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String) {
        val style = BeerStyles.canonical(name) ?: return
        viewModelScope.launch { dao.add(style) }
    }

    /** Suppression ; [replacement] obligatoire si le style est utilisé (vérifié aussi par le DAO). */
    fun remove(name: String, replacement: String?) {
        viewModelScope.launch { dao.remove(name, replacement) }
    }
}
