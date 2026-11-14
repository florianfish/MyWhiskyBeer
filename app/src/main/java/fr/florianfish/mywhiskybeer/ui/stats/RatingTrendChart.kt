package fr.florianfish.mywhiskybeer.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.domain.MonthPoint
import fr.florianfish.mywhiskybeer.domain.RatingScale
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Évolution de la note moyenne par mois : une seule série (pas de légende, le titre la nomme),
 * un seul axe, grille discrète, ligne 2 dp, points 8 dp cerclés du fond, seule la dernière
 * valeur étiquetée. Un appui sur un point affiche son mois et sa valeur.
 */
@Composable
fun RatingTrendChart(
    points: List<MonthPoint>,
    type: ProductType?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val measurer = rememberTextMeasurer()
    val grid = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val format: (Int) -> String = { RatingScale.format(it, type ?: ProductType.WHISKY) }

    // Axe des notes : de la dizaine sous le minimum (au plus 50) jusqu'à 100
    val minY = (floor(points.minOf { it.avgRating } / 10.0) * 10).toInt().coerceAtMost(50).coerceAtLeast(0)
    val maxY = (ceil(points.maxOf { it.avgRating } / 10.0) * 10).toInt().coerceAtLeast(minY + 10).coerceAtMost(100)

    Column(modifier) {
        val shown = selected?.let { points[it] } ?: points.last()
        Text(
            "${monthLabel(shown, long = true)} : ${format(shown.avgRating)} · ${shown.tastings} dégustation" +
                if (shown.tastings > 1) "s" else "",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics {
                    contentDescription = "Note moyenne par mois : " +
                        points.joinToString { "${monthLabel(it, long = true)} ${format(it.avgRating)}" }
                }
                .pointerInput(points) {
                    detectTapGestures { pos ->
                        val left = 36.dp.toPx()
                        val width = size.width - left - 12.dp.toPx()
                        val step = width / (points.size - 1)
                        val index = ((pos.x - left) / step).let { kotlin.math.round(it).toInt() }.coerceIn(0, points.lastIndex)
                        selected = if (selected == index) null else index
                    }
                },
        ) {
            val left = 36.dp.toPx()
            val right = 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = 22.dp.toPx()
            val w = size.width - left - right
            val h = size.height - top - bottom
            fun x(i: Int) = left + w * i / (points.size - 1)
            fun y(v: Int) = top + h * (1f - (v - minY).toFloat() / (maxY - minY))
            val labelStyle = TextStyle(color = muted, fontSize = 10.sp)

            // Grille horizontale discrète + libellés de l'axe
            val ticks = listOf(minY, (minY + maxY) / 2, maxY).distinct()
            ticks.forEach { v ->
                drawLine(grid, Offset(left, y(v)), Offset(size.width - right, y(v)), strokeWidth = 1.dp.toPx())
                val layout = measurer.measure(format(v).substringBefore('/'), labelStyle)
                drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y(v) - layout.size.height / 2f))
            }

            // Ligne
            val path = Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.avgRating)) else lineTo(x(i), y(p.avgRating)) }
            }
            drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Points : 8 dp, cerclés du fond pour se détacher de la ligne
            points.forEachIndexed { i, p ->
                val c = Offset(x(i), y(p.avgRating))
                val active = i == (selected ?: points.lastIndex)
                drawCircle(surface, radius = (if (active) 6 else 5).dp.toPx(), center = c)
                drawCircle(color, radius = (if (active) 4.5f else 4f).dp.toPx(), center = c)
            }

            // Mois : premier, dernier, et un sur deux entre les deux si la place manque
            val every = if (points.size > 6) 2 else 1
            points.forEachIndexed { i, p ->
                if (i == 0 || i == points.lastIndex || (i % every == 0 && abs(points.lastIndex - i) > 1)) {
                    val layout = measurer.measure(monthLabel(p, long = false), labelStyle)
                    val cx = (x(i) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                    drawText(layout, topLeft = Offset(cx, size.height - layout.size.height))
                }
            }

            // Étiquette directe : la valeur du point actif (le dernier par défaut)
            val activeIndex = selected ?: points.lastIndex
            val active = points[activeIndex]
            val valueLayout = measurer.measure(format(active.avgRating), TextStyle(color = ink, fontSize = 11.sp))
            val vx = (x(activeIndex) - valueLayout.size.width / 2f).coerceIn(left, size.width - valueLayout.size.width)
            val vy = (y(active.avgRating) - valueLayout.size.height - 8.dp.toPx()).coerceAtLeast(0f)
            drawText(valueLayout, topLeft = Offset(vx, vy))
        }
    }
}

private fun monthLabel(point: MonthPoint, long: Boolean): String {
    val name = point.month.month.getDisplayName(if (long) JavaTextStyle.FULL else JavaTextStyle.SHORT, Locale.FRENCH)
    return if (long) "$name ${point.month.year}" else name
}
