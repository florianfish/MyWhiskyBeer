package fr.florianfish.mywhiskybeer.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.florianfish.mywhiskybeer.data.local.entity.ProductSummary
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.domain.GroupSection
import fr.florianfish.mywhiskybeer.domain.RatingScale
import fr.florianfish.mywhiskybeer.domain.StatsReport
import fr.florianfish.mywhiskybeer.ui.components.AromaRadar
import fr.florianfish.mywhiskybeer.ui.components.AromaTag
import fr.florianfish.mywhiskybeer.ui.components.EmptyState
import fr.florianfish.mywhiskybeer.ui.components.GlassGauge
import fr.florianfish.mywhiskybeer.ui.components.TypeBadge
import fr.florianfish.mywhiskybeer.ui.components.TypeFilterChips
import fr.florianfish.mywhiskybeer.ui.theme.RatingTextStyle
import fr.florianfish.mywhiskybeer.ui.theme.accent

@Composable
fun StatsScreen(
    onProductClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val report = state.report
    // Couleur de la série : accent du type, ambre pour « Tout »
    val accent = type?.accent() ?: MaterialTheme.colorScheme.primary

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeFilterChips(type, viewModel::setType)
        }
        when {
            state.loading -> Unit
            report.tastingCount == 0 -> EmptyState(
                title = "Pas encore de chiffres",
                message = "Les statistiques apparaîtront après tes premières dégustations.",
            )
            else -> StatsContent(report, type, accent, onProductClick)
        }
    }
}

@Composable
private fun StatsContent(report: StatsReport, type: ProductType?, accent: Color, onProductClick: (Long) -> Unit) {
    val format: (Int) -> String = { RatingScale.format(it, type ?: ProductType.WHISKY) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Summary(report, format) }

        if (report.months.size >= 2) {
            item {
                StatsCard("Évolution de la note moyenne") {
                    RatingTrendChart(report.months, type, accent)
                }
            }
        }

        if (report.top.isNotEmpty()) {
            item {
                StatsCard("Top ${report.top.size}") {
                    report.top.forEachIndexed { index, item -> TopRow(index + 1, item, onProductClick) }
                }
            }
        }

        // Pas de profil sur « Tout » : les familles d'arômes diffèrent entre bière et whisky
        if (type != null) {
            item {
                StatsCard("Profil aromatique") {
                    if (report.aromaProfile.isEmpty()) {
                        Hint("Aucun arôme noté pour l'instant.")
                    } else {
                        AromaRadar(report.aromaProfile, color = accent)
                    }
                }
            }
        }

        report.sections.forEach { section ->
            item { GroupCard(section, accent, format) }
        }

        if (report.countries.isNotEmpty()) {
            item { Countries(report.countries) }
        }

        item {
            Text(
                "Notes moyennes sur l'ensemble des dégustations" +
                    if (type == null) ", bières et whiskies sur 100." else ".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Chiffres clés en tuiles : la valeur est le message, pas besoin de graphique. */
@Composable
private fun Summary(report: StatsReport, format: (Int) -> String) {
    // 2 × 2 : sur une seule ligne, « Dégustations » était tronqué sur un téléphone courant
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Dégustations", report.tastingCount.toString(), Modifier.weight(1f))
            Tile("Moyenne", report.avgRating?.let(format) ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Produits", report.productCount.toString(), Modifier.weight(1f))
            Tile("Pays", report.countryCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text(value, style = RatingTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Titre de carte un cran sous le titre d'écran (serif, mais plus petit)
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp))
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TopRow(rank: Int, item: ProductSummary, onClick: (Long) -> Unit) {
    val p = item.product
    val rating = item.avgRating?.toInt() ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick(p.id) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Column(Modifier.weight(1f)) {
            TypeBadge(p.type)
            Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            p.producer?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        GlassGauge(rating, p.type, size = 28.dp)
        Text(
            RatingScale.format(rating, p.type),
            style = MaterialTheme.typography.titleSmall,
            color = p.type.accent(),
            modifier = Modifier.width(56.dp),
        )
    }
}

/** Groupes préférés : barres horizontales fines, une seule teinte (magnitude), valeur en texte à côté. */
@Composable
private fun GroupCard(section: GroupSection, accent: Color, format: (Int) -> String) {
    StatsCard(section.title) {
        section.groups.forEach { g ->
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${format(g.avgRating)} · ${g.tastings}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(g.avgRating.coerceIn(0, 100) / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent),
                    )
                }
            }
        }
        Text(
            "Note moyenne · nombre de dégustations",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Countries(countries: List<Pair<String, Int>>) {
    StatsCard("Pays goûtés") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            countries.forEach { (name, count) -> AromaTag("$name · $count") }
        }
    }
}
