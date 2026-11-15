package fr.florianfish.mywhiskybeer.ui.cellar

import androidx.compose.foundation.clickable
import fr.florianfish.mywhiskybeer.ui.components.rememberFabScrollState
import fr.florianfish.mywhiskybeer.ui.components.ProductThumb
import fr.florianfish.mywhiskybeer.ui.components.BottleGauge
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    val fab = rememberFabScrollState()
    LaunchedEffect(selected) { fab.expanded = true }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().nestedScroll(fab.connection)) {
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
                // Type affiché seulement si la liste mélange bières et whiskies
                val mixed = list.map { it.product.type }.distinct().size > 1
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (tab == CellarTab.STOCK) {
                        item(key = "resume") { StockSummary(list) }
                    }
                    items(list, key = { it.item.productId }) { entry ->
                        CellarRow(entry, showType = mixed, onClick = { onProductClick(entry.product.id) })
                    }
                }
            }
        }
        // Dans la cave on ajoute des bouteilles (du stock), pas des dégustations
        ExtendedFloatingActionButton(
            onClick = { onAddBottle(tab == CellarTab.WISHLIST) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(if (tab == CellarTab.WISHLIST) "À acheter" else "Bouteille") },
            expanded = fab.expanded,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** Bouteille presque finie : signalée dans la liste et le résumé. */
private const val PRESQUE_FINIE = 15

private fun CellarItemWithProduct.isAlmostEmpty() =
    item.status == CellarStatus.OPENED && (item.fillLevel ?: 100) <= PRESQUE_FINIE

/** « 7 bouteilles · 6 entamées · 3 presque finies » en tête de l'onglet En stock. */
@Composable
private fun StockSummary(list: List<CellarItemWithProduct>) {
    val bottles = list.sumOf { it.item.quantity }
    val opened = list.count { it.item.status == CellarStatus.OPENED }
    val almostEmpty = list.count { it.isAlmostEmpty() }
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            listOfNotNull(
                if (bottles > 1) "$bottles bouteilles" else "$bottles bouteille",
                opened.takeIf { it > 0 }?.let { if (it > 1) "$it entamées" else "1 entamée" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (almostEmpty > 0) {
            Text(
                "· " + if (almostEmpty > 1) "$almostEmpty presque finies" else "1 presque finie",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun CellarRow(entry: CellarItemWithProduct, showType: Boolean, onClick: () -> Unit) {
    val p = entry.product
    val item = entry.item
    val accent = p.type.accent()
    ListItem(
        leadingContent = { ProductThumb(p.photoPath, p.type) },
        overlineContent = if (showType) { { TypeBadge(p.type) } } else null,
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
                when {
                    entry.isAlmostEmpty() -> Text(
                        "Presque finie",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    item.status == CellarStatus.OPENED -> Text("Entamée", style = MaterialTheme.typography.labelMedium, color = accent)
                    item.status == CellarStatus.IN_STOCK -> Text(
                        "Fermée",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Unit
                }
            }
        },
        trailingContent = when (item.status) {
            CellarStatus.IN_STOCK, CellarStatus.OPENED -> {{
                // Niveau de la bouteille : pleine si fermée
                val level = if (item.status == CellarStatus.IN_STOCK) 100 else item.fillLevel
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BottleGauge(
                        level = level ?: 100,
                        color = if (entry.isAlmostEmpty()) MaterialTheme.colorScheme.tertiary else accent,
                        height = 40.dp,
                    )
                    if (item.status == CellarStatus.OPENED && level != null) {
                        Text(
                            "$level %",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (entry.isAlmostEmpty()) MaterialTheme.colorScheme.tertiary else accent,
                        )
                    }
                }
            }}
            else -> null
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
