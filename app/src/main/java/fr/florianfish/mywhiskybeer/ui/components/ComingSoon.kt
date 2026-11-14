package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Écran provisoire pour les fonctionnalités pas encore développées. */
@Composable
fun ComingSoon(title: String, modifier: Modifier = Modifier) {
    EmptyState(title = title, message = "Bientôt dans ton verre.", modifier = modifier)
}
