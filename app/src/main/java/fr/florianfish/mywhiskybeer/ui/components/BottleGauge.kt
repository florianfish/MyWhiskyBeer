package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Niveau d'une bouteille en cave (0..100), dessiné comme une bouteille qui se vide.
 * Pendant de [GlassGauge] (note) pour le stock.
 */
@Composable
fun BottleGauge(
    level: Int,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val fill by animateFloatAsState(level.coerceIn(0, 100) / 100f, tween(600), label = "bottleLevel")
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(width = height * 0.45f, height = height)) {
        val bottle = bottlePath(size)
        val stroke = size.width * 0.09f
        // Le liquide occupe le corps de la bouteille (le col reste vide, comme une vraie bouteille)
        val bodyTop = size.height * BODY_TOP
        val liquidTop = size.height - (size.height - bodyTop) * fill
        clipPath(bottle) {
            drawRect(color, topLeft = Offset(0f, liquidTop), size = Size(size.width, size.height - liquidTop))
        }
        drawPath(bottle, outline, style = Stroke(width = stroke, join = StrokeJoin.Round))
    }
}

/** Haut du corps de la bouteille, en fraction de la hauteur. */
private const val BODY_TOP = 0.42f

/** Silhouette de bouteille de whisky : col étroit, épaules arrondies, corps droit. */
internal fun DrawScope.bottlePath(size: Size): Path {
    val w = size.width
    val h = size.height
    val inset = w * 0.06f
    val neckHalf = w * 0.17f
    val cx = w / 2
    val shoulder = h * BODY_TOP
    val r = w * 0.12f
    return Path().apply {
        moveTo(cx - neckHalf, inset)
        lineTo(cx + neckHalf, inset)
        lineTo(cx + neckHalf, h * 0.24f)
        quadraticTo(w - inset, h * 0.28f, w - inset, shoulder)
        lineTo(w - inset, h - inset - r)
        quadraticTo(w - inset, h - inset, w - inset - r, h - inset)
        lineTo(inset + r, h - inset)
        quadraticTo(inset, h - inset, inset, h - inset - r)
        lineTo(inset, shoulder)
        quadraticTo(inset, h * 0.28f, cx - neckHalf, h * 0.24f)
        close()
    }
}
