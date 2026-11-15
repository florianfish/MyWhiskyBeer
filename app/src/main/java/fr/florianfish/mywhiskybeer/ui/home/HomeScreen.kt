package fr.florianfish.mywhiskybeer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.ui.components.EmptyState
import fr.florianfish.mywhiskybeer.ui.components.TastingCard
import fr.florianfish.mywhiskybeer.ui.components.StyleFilterChips
import fr.florianfish.mywhiskybeer.ui.components.TypeFilterChips

@Composable
fun HomeScreen(
    onTastingClick: (productId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    when (val s = state) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        // Aucune dégustation du tout : pas de filtre à proposer
        is HomeUiState.Ready -> if (s.total == 0) {
            EmptyState(
                title = "Rien dans les verres",
                message = "Note ta première bière ou ton premier whisky avec le bouton « Déguster ».",
                modifier = modifier,
            )
        } else {
            Column(modifier.fillMaxSize()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeFilterChips(filter, viewModel::setFilter)
                }
                if (filter == ProductType.BEER) {
                    val styles by viewModel.beerStyles.collectAsStateWithLifecycle()
                    val style by viewModel.style.collectAsStateWithLifecycle()
                    StyleFilterChips(styles, style, viewModel::setStyle, Modifier.padding(bottom = 4.dp))
                }
                if (s.latest.isEmpty()) {
                    EmptyState(
                        title = "Aucune dégustation",
                        message = if (filter == ProductType.BEER) "Pas encore de bière notée." else "Pas encore de whisky noté.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Type affiché seulement si la liste mélange bières et whiskies (le liseré garde la couleur)
                        val mixed = s.latest.map { it.product.type }.distinct().size > 1
                        items(s.latest, key = { it.tasting.id }) { item ->
                            TastingCard(item = item, showType = mixed, onClick = { onTastingClick(item.product.id) })
                        }
                    }
                }
            }
        }
    }
}
