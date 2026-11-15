package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType

/**
 * Filtre Tout / Whisky / Bière, commun aux onglets Dégustations, Carnet et Stats.
 * À placer dans une Row. selected = null pour « Tout ».
 */
@Composable
fun TypeFilterChips(selected: ProductType?, onSelect: (ProductType?) -> Unit) {
    listOf(null to "Tout", ProductType.WHISKY to "Whisky", ProductType.BEER to "Bière").forEach { (type, label) ->
        FilterChip(selected = selected == type, onClick = { onSelect(type) }, label = { Text(label) })
    }
}
