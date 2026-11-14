package fr.florianfish.mywhiskybeer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.TastingWithProduct
import fr.florianfish.mywhiskybeer.domain.RatingScale
import fr.florianfish.mywhiskybeer.ui.theme.RatingTextStyle
import fr.florianfish.mywhiskybeer.ui.theme.accent
import java.text.DateFormat
import java.util.Date

/** Carte façon étiquette : liseré de couleur du type, nom en serif, verre-jauge à droite. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TastingCard(
    item: TastingWithProduct,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = item.product.type
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(type.accent()),
            )
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TypeBadge(type)
                    Text(
                        item.product.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = listOfNotNull(
                        item.product.producer,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.tasting.date)),
                    ).joinToString(" · ")
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.aromas.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            item.aromas.take(4).forEach { AromaTag(it.name) }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassGauge(rating = item.tasting.rating, type = type, size = 44.dp)
                    Text(
                        RatingScale.format(item.tasting.rating, type),
                        style = RatingTextStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                        color = type.accent(),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun TypeBadge(type: ProductType, modifier: Modifier = Modifier) {
    Text(
        text = when (type) {
            ProductType.BEER -> "BIÈRE"
            ProductType.WHISKY -> "WHISKY"
        },
        style = MaterialTheme.typography.labelSmall,
        color = type.accent(),
        modifier = modifier,
    )
}

@Composable
fun AromaTag(name: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
