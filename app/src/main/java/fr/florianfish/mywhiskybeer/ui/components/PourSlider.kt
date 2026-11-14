package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.domain.RatingScale
import fr.florianfish.mywhiskybeer.ui.theme.RatingTextStyle
import fr.florianfish.mywhiskybeer.ui.theme.accent

/**
 * Curseur « verser » : on fait glisser le doigt vers le haut sur le verre pour le remplir.
 * Vibration à chaque demi-point (bière) ou tous les 5 points (whisky). Boutons −/+ pour l'ajustement fin.
 */
@Composable
fun PourSlider(
    rating: Int,
    type: ProductType,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentRating by rememberUpdatedState(rating)
    val currentOnChange by rememberUpdatedState(onRatingChange)
    var dragging by remember { mutableStateOf(false) }
    val step = RatingScale.step(type)

    fun update(y: Float, height: Int) {
        if (height <= 0) return
        val snapped = RatingScale.snap((1f - y / height) * 100f, type)
        if (snapped != currentRating) {
            if (type == ProductType.BEER || snapped % 5 == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            currentOnChange(snapped)
        }
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FilledTonalIconButton(onClick = { currentOnChange((rating - step).coerceAtLeast(0)) }) {
                Icon(Icons.Outlined.Remove, contentDescription = "Baisser la note")
            }
            Box(
                Modifier
                    .size(width = 130.dp, height = 160.dp)
                    .semantics { contentDescription = "Note : ${RatingScale.format(rating, type)}. Glisser pour remplir le verre." }
                    .pointerInput(type) {
                        detectTapGestures { update(it.y, size.height) }
                    }
                    .pointerInput(type) {
                        detectVerticalDragGestures(
                            onDragStart = { dragging = true; update(it.y, size.height) },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, _ ->
                            change.consume()
                            update(change.position.y, size.height)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                GlassGauge(rating = rating, type = type, size = 150.dp, animate = !dragging)
            }
            FilledTonalIconButton(onClick = { currentOnChange((rating + step).coerceAtMost(100)) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Monter la note")
            }
        }
        Text(
            RatingScale.format(rating, type),
            style = RatingTextStyle.copy(fontSize = 40.sp),
            color = type.accent(),
        )
        Text(
            RatingScale.label(rating),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
