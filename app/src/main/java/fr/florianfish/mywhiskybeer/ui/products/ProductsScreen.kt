package fr.florianfish.mywhiskybeer.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.ui.components.EmptyState
import fr.florianfish.mywhiskybeer.ui.components.TypeBadge

@Composable
fun ProductsScreen(
    onProductClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { viewModel.setFilter(null) }, label = { Text("Tout") })
            FilterChip(
                selected = filter == ProductType.WHISKY,
                onClick = { viewModel.setFilter(ProductType.WHISKY) },
                label = { Text("Whisky") },
            )
            FilterChip(
                selected = filter == ProductType.BEER,
                onClick = { viewModel.setFilter(ProductType.BEER) },
                label = { Text("Bière") },
            )
        }

        if (products.isEmpty()) {
            EmptyState(title = "Carnet vide", message = "Les produits notés apparaîtront ici.")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(products, key = { it.product.id }) { item ->
                    ProductRow(item, onClick = { onProductClick(item.product.id) })
                }
            }
        }
    }
}

@Composable
private fun ProductRow(item: ProductWithDetails, onClick: () -> Unit) {
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
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
