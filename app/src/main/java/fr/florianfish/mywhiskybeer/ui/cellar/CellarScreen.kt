package fr.florianfish.mywhiskybeer.ui.cellar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemWithProduct
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import fr.florianfish.mywhiskybeer.domain.formatDecimal
import fr.florianfish.mywhiskybeer.ui.components.EmptyState
import fr.florianfish.mywhiskybeer.ui.components.TypeBadge
import fr.florianfish.mywhiskybeer.ui.theme.accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellarScreen(
    onProductClick: (Long) -> Unit,
    onAddBottle: (wishlist: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CellarViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tab = CellarTab.entries[selected]

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = selected, containerColor = MaterialTheme.colorScheme.background) {
                CellarTab.entries.forEachIndexed { i, t ->
                    val count = items[t]?.size ?: 0
                    Tab(
                        selected = selected == i,
                        onClick = { selected = i },
                        text = { Text(if (count > 0) "${t.label} ($count)" else t.label) },
                    )
                }
            }
            val list = items[tab].orEmpty()
            if (list.isEmpty()) {
                EmptyState(
                    title = when (tab) {
                        CellarTab.STOCK -> "Cave vide"
                        CellarTab.WISHLIST -> "Rien à acheter"
                        CellarTab.EMPTY -> "Aucune bouteille finie"
                    },
                    message = when (tab) {
                        CellarTab.WISHLIST -> "Note les bouteilles que tu veux acheter avec le bouton « À acheter »."
                        else -> "Ajoute tes bouteilles avec le bouton « Bouteille », ou scanne directement l'étiquette."
                    },
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(list, key = { it.item.productId }) { entry ->
                        CellarRow(entry, onClick = { onProductClick(entry.product.id) })
                    }
                }
            }
        }
        // Dans la cave on ajoute des bouteilles (du stock), pas des dégustations
        ExtendedFloatingActionButton(
            onClick = { onAddBottle(tab == CellarTab.WISHLIST) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(if (tab == CellarTab.WISHLIST) "À acheter" else "Bouteille") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun CellarRow(entry: CellarItemWithProduct, onClick: () -> Unit) {
    val p = entry.product
    val item = entry.item
    ListItem(
        overlineContent = { TypeBadge(p.type) },
        headlineContent = { Text(p.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Column {
                val details = listOfNotNull(
                    p.producer,
                    item.quantity.takeIf { it > 1 }?.let { "× $it" },
                    item.purchasePrice?.let { "${formatDecimal(it)} €" },
                    item.shop,
                )
                if (details.isNotEmpty()) Text(details.joinToString(" · "))
                if (item.status == CellarStatus.OPENED) {
                    val level = item.fillLevel
                    Text(
                        if (level != null) "Entamée · $level %" else "Entamée",
                        style = MaterialTheme.typography.labelMedium,
                        color = p.type.accent(),
                    )
                    if (level != null) {
                        LinearProgressIndicator(
                            progress = { level / 100f },
                            color = p.type.accent(),
                            modifier = Modifier.padding(top = 4.dp).width(120.dp),
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
