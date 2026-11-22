package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.ui.theme.accent
import java.io.File

/**
 * Vignette d'un produit dans les listes : sa photo, sinon une silhouette
 * (bouteille pour le whisky, verre pour la bière) dans la couleur du type.
 */
@Composable
fun ProductThumb(photoPath: String?, type: ProductType, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val color = type.accent().copy(alpha = 0.7f)
            when (type) {
                ProductType.WHISKY -> Canvas(Modifier.size(width = size * 0.3f, height = size * 0.66f)) {
                    drawPath(bottlePath(this.size), color, style = Stroke(width = this.size.width * 0.1f))
                }
                // Même teinte que la bouteille : le verre vide n'est pas dessiné à l'encre
                ProductType.BEER -> GlassGauge(rating = 0, type = type, size = size * 0.6f, outline = color, modifier = Modifier.padding(2.dp))
            }
        }
    }
}
