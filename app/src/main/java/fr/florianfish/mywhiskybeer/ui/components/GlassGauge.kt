package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import androidx.compose.ui.graphics.Color
import fr.florianfish.mywhiskybeer.ui.theme.Foam
import fr.florianfish.mywhiskybeer.ui.theme.liquid

/**
 * Jauge de note en forme de verre qui se remplit (0..100).
 * Tumbler pour le whisky, verre à bière évasé pour la bière.
 * Élément signature de l'appli, à la place des étoiles.
 */
@Composable
fun GlassGauge(
    rating: Int,
    type: ProductType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    /** false pendant un glisser du doigt : le verre suit sans retard. */
    animate: Boolean = true,
    /** Contour du verre ; par défaut la couleur du texte. */
    outline: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
) {
    val level by animateFloatAsState(
        targetValue = rating.coerceIn(0, 100) / 100f,
        animationSpec = if (animate) tween(durationMillis = 600) else snap(),
        label = "glassLevel",
    )
    val liquid = type.liquid()
    val glass = outline
    // Mousse crème dans les deux modes (en onSurface, elle était noire en mode clair)
    val foam = Foam

    Canvas(modifier.size(width = size * 0.8f, height = size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.07f).coerceAtLeast(1.5f)

        // Silhouette : plus évasée pour la bière, quasi droite pour le tumbler
        val topInset = if (type == ProductType.BEER) w * 0.04f else w * 0.08f
        val bottomInset = if (type == ProductType.BEER) w * 0.22f else w * 0.14f
        val top = stroke
        val bottom = h - stroke

        val outline = Path().apply {
            moveTo(topInset, top)
            lineTo(w - topInset, top)
            lineTo(w - bottomInset, bottom)
            lineTo(bottomInset, bottom)
            close()
        }

        // Liquide, découpé dans la silhouette du verre
        val liquidTop = bottom - (bottom - top) * level
        clipPath(outline) {
            drawRect(
                color = liquid,
                topLeft = Offset(0f, liquidTop),
                size = Size(w, bottom - liquidTop),
            )
            if (type == ProductType.BEER && level > 0.08f) {
                // Col de mousse
                drawRect(
                    color = foam,
                    topLeft = Offset(0f, liquidTop - h * 0.06f),
                    size = Size(w, h * 0.06f),
                )
            }
        }

        drawPath(outline, color = glass, style = Stroke(width = stroke, join = StrokeJoin.Round))
        // Fond épais du verre
        drawLine(
            color = glass,
            start = Offset(bottomInset, bottom),
            end = Offset(w - bottomInset, bottom),
            strokeWidth = stroke * 2f,
        )
    }
}
