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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType

// Tous les rôles sont définis : un rôle oublié retombe sur la palette violette
// par défaut de Material (ex. tertiaryContainer, surfaceContainerHighest en clair).
// secondaryContainer n'est PAS la paille : Material l'emploie pour toute sélection
// (onglet actif, FilterChip, bouton tonal, SegmentedButton) ; en paille, « Tout »
// sélectionné aurait l'air de signifier « Bière ». On y met un cuir neutre.
private val DarkColors = darkColorScheme(
    primary = Amber, onPrimary = OnAmber,
    primaryContainer = Color(0xFF5A3510), onPrimaryContainer = Color(0xFFFFDDB8),
    inversePrimary = AmberDeep,
    secondary = Straw, onSecondary = OnStraw,
    secondaryContainer = Leather, onSecondaryContainer = OnLeather,
    tertiary = Copper, onTertiary = OnCopper,
    tertiaryContainer = Color(0xFF5C2A14), onTertiaryContainer = Color(0xFFFFDBCC),
    background = Cave, onBackground = Parchment,
    surface = Cave, onSurface = Parchment,
    surfaceVariant = CaveSurfaceHigh, onSurfaceVariant = ParchmentMuted,
    surfaceTint = Amber,
    inverseSurface = Parchment, inverseOnSurface = Cave,
    surfaceDim = Cave,
    surfaceBright = Color(0xFF3A3027),
    surfaceContainerLowest = Color(0xFF120E0A),
    surfaceContainerLow = Color(0xFF1C1611),
    surfaceContainer = CaveSurface,
    surfaceContainerHigh = Color(0xFF292119),
    surfaceContainerHighest = CaveSurfaceHigh,
    outline = Outline, outlineVariant = Color(0xFF43372C),
    error = ErrorDark, onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = AmberDeep, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB8), onPrimaryContainer = Color(0xFF2E1500),
    inversePrimary = Amber,
    secondary = StrawDeep, onSecondary = Color.White,
    secondaryContainer = LeatherLight, onSecondaryContainer = OnLeatherLight,
    tertiary = CopperDeep, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCC), onTertiaryContainer = Color(0xFF3A0F00),
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = PaperSurfaceHigh, onSurfaceVariant = InkMuted,
    surfaceTint = AmberDeep,
    inverseSurface = Color(0xFF3A2F26), inverseOnSurface = Paper,
    surfaceDim = Color(0xFFE6DCCD),
    surfaceBright = Paper,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFDF8F0),
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceHigh,
    surfaceContainerHighest = Color(0xFFEADDCA),
    outline = InkMuted, outlineVariant = Color(0xFFD8C8B4),
    error = ErrorLight, onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    scrim = Color.Black,
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

/**
 * Couleur du liquide dans les jauges (verre, bouteille) : l'accent en sombre ;
 * en clair, une teinte plus lumineuse que l'accent (fait pour le texte sur papier).
 */
@Composable
@ReadOnlyComposable
fun ProductType.liquid(): Color {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return when {
        !light -> accent()
        this == ProductType.WHISKY -> AmberLiquid
        else -> StrawLiquid
    }
}
