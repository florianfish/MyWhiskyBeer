package fr.florianfish.mywhiskybeer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType

private val DarkColors = darkColorScheme(
    primary = Amber, onPrimary = OnAmber,
    primaryContainer = Color(0xFF5A3510), onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Straw, onSecondary = OnStraw,
    secondaryContainer = Color(0xFF4F3F00), onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Copper, onTertiary = OnCopper,
    background = Cave, onBackground = Parchment,
    surface = Cave, onSurface = Parchment,
    surfaceVariant = CaveSurfaceHigh, onSurfaceVariant = ParchmentMuted,
    surfaceContainerLowest = Color(0xFF120E0A),
    surfaceContainerLow = Color(0xFF1C1611),
    surfaceContainer = CaveSurface,
    surfaceContainerHigh = Color(0xFF292119),
    surfaceContainerHighest = CaveSurfaceHigh,
    outline = Outline, outlineVariant = Color(0xFF43372C),
    error = ErrorDark,
)

private val LightColors = lightColorScheme(
    primary = AmberDeep, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB8), onPrimaryContainer = Color(0xFF2E1500),
    secondary = StrawDeep, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A), onSecondaryContainer = Color(0xFF241A00),
    tertiary = CopperDeep, onTertiary = Color.White,
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = PaperSurfaceHigh, onSurfaceVariant = InkMuted,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceHigh,
    outline = InkMuted,
    error = ErrorLight,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
)

/**
 * Couleurs dynamiques (Material You) volontairement désactivées :
 * l'identité ambre / paille fait partie de l'appli.
 */
@Composable
fun MyWhiskyBeerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** Couleur d'accent d'un type de produit : ambre pour le whisky, paille pour la bière. */
@Composable
@ReadOnlyComposable
fun ProductType.accent(): Color = when (this) {
    ProductType.WHISKY -> MaterialTheme.colorScheme.primary
    ProductType.BEER -> MaterialTheme.colorScheme.secondary
}
