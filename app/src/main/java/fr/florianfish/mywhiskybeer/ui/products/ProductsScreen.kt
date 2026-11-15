package fr.florianfish.mywhiskybeer.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.florianfish.mywhiskybeer.data.local.entity.ProductSummary
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.domain.ProductListing
import fr.florianfish.mywhiskybeer.domain.ProductSort
import fr.florianfish.mywhiskybeer.domain.RatingScale
import fr.florianfish.mywhiskybeer.ui.components.EmptyState
import fr.florianfish.mywhiskybeer.ui.components.GlassGauge
import fr.florianfish.mywhiskybeer.ui.components.StyleFilterChips
import fr.florianfish.mywhiskybeer.ui.components.TypeBadge
import fr.florianfish.mywhiskybeer.ui.components.TypeFilterChips
import fr.florianfish.mywhiskybeer.ui.theme.accent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
    onProductClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Nom, distillerie, région, style…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Effacer la recherche")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            Modifier.padding(start = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeFilterChips(filter, viewModel::setFilter)
            Spacer(Modifier.weight(1f))
            SortMenu(sort, onSort = viewModel::setSort)
        }
        if (filter == ProductType.BEER) {
            StyleFilterChips(state.beerStyles, style, viewModel::setStyle, Modifier.padding(bottom = 4.dp))
        }

        when {
            state.loading -> Unit
            state.total == 0 -> EmptyState(title = "Carnet vide", message = "Les produits notés apparaîtront ici.")
            state.products.isEmpty() -> EmptyState(
                title = "Aucun résultat",
                message = when {
                    query.isNotBlank() -> "Rien ne correspond à « ${query.trim()} »."
                    style != null -> "Aucune bière de style $style."
                    else -> "Aucun produit de ce type."
                },
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                if (sort == ProductSort.STYLE) {
                    // Sections par style (bière) ou catégorie (whisky), dans l'ordre du tri
                    state.products
                        .groupBy { "${it.product.type}|${ProductListing.groupKey(it)}" }
                        .forEach { (key, items) ->
                            stickyHeader(key = key) {
                                GroupHeader(ProductListing.groupKey(items.first()), items.size, items.first().product.type)
                            }
                            items(items, key = { it.product.id }) { item ->
                                ProductRow(item, onClick = { onProductClick(item.product.id) })
                            }
                        }
                } else {
                    items(state.products, key = { it.product.id }) { item ->
                        ProductRow(item, onClick = { onProductClick(item.product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(sort: ProductSort, onSort: (ProductSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(sort.label)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ProductSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            color = if (option == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { open = false; onSort(option) },
                )
            }
        }
    }
}

@Composable
private fun ProductRow(item: ProductSummary, onClick: () -> Unit) {
    val p = item.product
    val details = when (p.type) {
        ProductType.BEER -> listOfNotNull(item.beer?.style, p.abv?.let { "$it %" })
        ProductType.WHISKY -> listOfNotNull(
            item.whisky?.age?.let { "$it ans" },
            item.whisky?.caskType,
            p.abv?.let { "$it %" },
        )
    }
    ListItem(
        overlineContent = { TypeBadge(p.type) },
        headlineContent = { Text(p.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(listOfNotNull(p.producer, p.region ?: p.country).plus(details).joinToString(" · "))
        },
        trailingContent = {
            val avg = item.avgRating
            if (avg != null) {
                val rating = avg.toInt()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassGauge(rating = rating, type = p.type, size = 28.dp)
                    Text(
                        RatingScale.format(rating, p.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = p.type.accent(),
                    )
                    if (item.tastingCount > 1) {
                        Text(
                            "× ${item.tastingCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text("À goûter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun GroupHeader(title: String, count: Int, type: ProductType) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = type.accent(), modifier = Modifier.weight(1f))
        Text(
            if (count > 1) "$count produits" else "1 produit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
