package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Radar du profil aromatique : un axe par famille, valeurs entre 0 et 1.
 */
@Composable
fun AromaRadar(
    values: List<Pair<String, Float>>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier.fillMaxWidth().aspectRatio(1.15f)) {
        if (values.size < 3) return@Canvas
        val labelSpace = 44.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f - labelSpace
        val n = values.size

        fun point(i: Int, r: Float): Offset {
            val a = Math.toRadians(-90.0 + 360.0 * i / n)
            return Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
        }

        fun polygon(r: (Int) -> Float) = Path().apply {
            for (i in 0 until n) {
                val p = point(i, r(i))
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }

        // Grille
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { level ->
            drawPath(polygon { radius * level }, grid, style = Stroke(width = 1.dp.toPx()))
        }
        for (i in 0 until n) drawLine(grid, center, point(i, radius), strokeWidth = 1.dp.toPx())

        // Profil
        val shape = polygon { radius * values[it].second.coerceIn(0f, 1f) }
        drawPath(shape, color.copy(alpha = 0.35f))
        drawPath(shape, color, style = Stroke(width = 2.dp.toPx()))
        for (i in 0 until n) drawCircle(color, radius = 3.dp.toPx(), center = point(i, radius * values[i].second.coerceIn(0f, 1f)))

        // Libellés
        val maxLabel = (labelSpace * 2).toInt()
        values.forEachIndexed { i, (label, _) ->
            val layout = textMeasurer.measure(
                label,
                style = TextStyle(color = labelColor, fontSize = 11.sp, textAlign = TextAlign.Center),
                constraints = Constraints(maxWidth = maxLabel),
            )
            val p = point(i, radius + 8.dp.toPx())
            val dx = p.x - center.x
            val x = when {
                dx > 4f -> p.x
                dx < -4f -> p.x - layout.size.width
                else -> p.x - layout.size.width / 2f
            }
            val dy = p.y - center.y
            val y = when {
                dy > 4f -> p.y
                dy < -4f -> p.y - layout.size.height
                else -> p.y - layout.size.height / 2f
            }
            drawText(layout, topLeft = Offset(x, y))
        }
    }
}
