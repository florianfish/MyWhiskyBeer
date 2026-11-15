package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Bouton flottant qui se réduit à son icône quand on fait défiler vers le bas
 * (pour ne pas masquer la liste) et reprend son texte en remontant.
 * À brancher avec `Modifier.nestedScroll(state.connection)` sur le contenu qui défile.
 */
@Stable
class FabScrollState {
    var expanded by mutableStateOf(true)

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -SEUIL) expanded = false else if (available.y > SEUIL) expanded = true
            return Offset.Zero
        }
    }

    private companion object {
        const val SEUIL = 1f
    }
}

@Composable
fun rememberFabScrollState(): FabScrollState = remember { FabScrollState() }
