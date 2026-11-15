package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Filtre par style de bière (IPA, NEIPA…), en ligne défilante sous le filtre Bière.
 * selected = null pour « Tous les styles ». Rien n'est affiché sans style connu.
 */
@Composable
fun StyleFilterChips(
    styles: List<Pair<String, Int>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (styles.isEmpty()) return
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Tous les styles") })
        styles.forEach { (style, count) ->
            FilterChip(
                selected = selected == style,
                onClick = { onSelect(if (selected == style) null else style) },
                label = { Text("$style · $count") },
            )
        }
    }
}
