package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/** Teintes chaudes pour distinguer les familles d'arômes sur la roue et le radar. */
val FamilyPalette = listOf(
    Color(0xFFE0A458), // ambre
    Color(0xFFD9B44A), // or
    Color(0xFFC9704A), // cuivre
    Color(0xFF9DB06A), // olive
    Color(0xFFC77D8E), // vieux rose
    Color(0xFF8C6A52), // tabac
    Color(0xFF7FA79B), // sauge
    Color(0xFF7D8FB3), // ardoise
)

/**
 * Roue d'arômes : familles sur l'anneau intérieur, arômes sur l'anneau extérieur.
 * Un appui sur un arôme fait défiler son intensité (léger → marqué → dominant → aucun).
 */
@Composable
fun AromaWheel(
    aromas: List<AromaEntity>,
    selected: Map<Long, Int>,
    onAromaTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val families = remember(aromas) { aromas.groupBy { it.family }.toList() }
    // Ordre des arômes autour de la roue : famille par famille
    val ordered = remember(families) { families.flatMap { it.second } }
    val haptic = LocalHapticFeedback.current
    val currentOnTap by rememberUpdatedState(onAromaTap)
    val textMeasurer = rememberTextMeasurer()

    val background = MaterialTheme.colorScheme.background
    val empty = MaterialTheme.colorScheme.surfaceContainerHigh
    val text = MaterialTheme.colorScheme.onSurface
    val selectedText = MaterialTheme.colorScheme.background

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(ordered) {
                detectTapGestures { pos ->
                    if (ordered.isEmpty()) return@detectTapGestures
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) / 2f
                    val dx = pos.x - center.x
                    val dy = pos.y - center.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < radius * FAMILY_OUTER || dist > radius) return@detectTapGestures
                    // Angle depuis midi, dans le sens horaire
                    val deg = (Math.toDegrees(atan2(dy, dx).toDouble()) + 90.0 + 360.0) % 360.0
                    val index = floor(deg / (360.0 / ordered.size)).toInt().coerceIn(0, ordered.lastIndex)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnTap(ordered[index].id)
                }
            },
    ) {
        if (ordered.isEmpty()) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f
        val rHole = radius * HOLE
        val rFamily = radius * FAMILY_OUTER
        val sweep = 360f / ordered.size
        val gap = 1.5.dp.toPx()

        var index = 0
        families.forEachIndexed { familyIndex, (family, items) ->
            val color = FamilyPalette[familyIndex % FamilyPalette.size]
            val familyStart = -90f + index * sweep
            val familySweep = items.size * sweep

            // Anneau intérieur : la famille
            drawPath(ringSegment(center, rHole, rFamily, familyStart, familySweep), color.copy(alpha = 0.85f))
            drawPath(
                ringSegment(center, rHole, rFamily, familyStart, familySweep),
                background,
                style = Stroke(width = gap * 2),
            )
            drawRadialLabel(
                textMeasurer, family, center, rHole, rFamily, familyStart + familySweep / 2f,
                TextStyle(color = Color(0xFF1A130D), fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
            )

            // Anneau extérieur : les arômes
            items.forEach { aroma ->
                val start = -90f + index * sweep
                val intensity = selected[aroma.id] ?: 0
                val fill = if (intensity == 0) empty else color.copy(alpha = 0.45f + 0.18f * intensity)
                val segment = ringSegment(center, rFamily + gap, radius, start, sweep)
                drawPath(segment, fill)
                drawPath(segment, background, style = Stroke(width = gap * 2))
                drawRadialLabel(
                    textMeasurer, aroma.name, center, rFamily + gap, radius, start + sweep / 2f,
                    TextStyle(
                        color = if (intensity == 0) text else selectedText,
                        fontSize = 10.sp,
                        fontWeight = if (intensity >= 2) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                index++
            }
        }
    }
}

private const val HOLE = 0.16f
private const val FAMILY_OUTER = 0.44f

private fun ringSegment(center: Offset, rInner: Float, rOuter: Float, start: Float, sweep: Float) =
    Path().apply {
        arcTo(Rect(center, rOuter), start, sweep, forceMoveTo = true)
        arcTo(Rect(center, rInner), start + sweep, -sweep, forceMoveTo = false)
        close()
    }

/** Texte orienté le long du rayon, retourné sur la moitié gauche pour rester lisible. */
private fun DrawScope.drawRadialLabel(
    measurer: TextMeasurer,
    label: String,
    center: Offset,
    rInner: Float,
    rOuter: Float,
    angle: Float,
    style: TextStyle,
) {
    val padding = 4.dp.toPx()
    val maxWidth = (rOuter - rInner - 2 * padding).toInt().coerceAtLeast(1)
    val layout = measurer.measure(
        label,
        style = style,
        constraints = Constraints(maxWidth = maxWidth),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    val normalized = ((angle % 360f) + 360f) % 360f
    val leftHalf = normalized > 90f && normalized < 270f
    val y = center.y - layout.size.height / 2f
    if (!leftHalf) {
        rotate(angle, pivot = center) {
            drawText(layout, topLeft = Offset(center.x + rInner + padding, y))
        }
    } else {
        rotate(angle + 180f, pivot = center) {
            drawText(layout, topLeft = Offset(center.x - rInner - padding - layout.size.width, y))
        }
    }
}
