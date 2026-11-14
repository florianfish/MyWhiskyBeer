package fr.florianfish.mywhiskybeer.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import fr.florianfish.mywhiskybeer.data.local.entity.PeatLevel
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.local.entity.TastingWithProduct
import fr.florianfish.mywhiskybeer.domain.RatingScale
import fr.florianfish.mywhiskybeer.ui.components.AromaRadar
import fr.florianfish.mywhiskybeer.ui.components.AromaTag
import fr.florianfish.mywhiskybeer.ui.components.EmptyState
import fr.florianfish.mywhiskybeer.ui.components.GlassGauge
import fr.florianfish.mywhiskybeer.ui.components.TypeBadge
import fr.florianfish.mywhiskybeer.ui.theme.RatingTextStyle
import fr.florianfish.mywhiskybeer.ui.theme.accent
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onTasteAgain: (productId: Long) -> Unit,
    onEditStock: (productId: Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteProduct by remember { mutableStateOf(false) }
    var tastingToDelete by remember { mutableStateOf<TastingWithProduct?>(null) }

    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Plus") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Supprimer le produit") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = { menuOpen = false; confirmDeleteProduct = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            state.product?.let { p ->
                ExtendedFloatingActionButton(
                    onClick = { onTasteAgain(p.product.id) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Déguster à nouveau") },
                    containerColor = p.product.type.accent(),
                    contentColor = MaterialTheme.colorScheme.background,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val product = state.product
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            product == null -> EmptyState("Produit introuvable", "Il a peut-être été supprimé.", Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Header(product, state.averageRating, state.tastings.size, topPadding = padding.calculateTopPadding()) }
                item { DetailsBlock(product) }
                item {
                    CellarBlock(
                        type = product.product.type,
                        cellar = state.cellar,
                        onStatus = viewModel::setCellarStatus,
                        onFillLevel = viewModel::setFillLevel,
                        onEdit = { onEditStock(product.product.id) },
                    )
                }
                if (state.profile.isNotEmpty()) {
                    item {
                        Section("Profil aromatique") {
                            AromaRadar(state.profile, color = product.product.type.accent())
                        }
                    }
                }
                item {
                    Text(
                        "Dégustations",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                items(state.tastings, key = { it.tasting.id }) { item ->
                    TastingItem(item, onDelete = { tastingToDelete = item }, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (confirmDeleteProduct) {
        AlertDialog(
            onDismissRequest = { confirmDeleteProduct = false },
            title = { Text("Supprimer ce produit ?") },
            text = { Text("Ses dégustations, ses photos et sa place en cave seront supprimées définitivement.") },
            confirmButton = {
                TextButton(onClick = { confirmDeleteProduct = false; viewModel.deleteProduct() }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteProduct = false }) { Text("Annuler") } },
        )
    }
    tastingToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { tastingToDelete = null },
            title = { Text("Supprimer cette dégustation ?") },
            text = { Text("Elle sera supprimée définitivement.") },
            confirmButton = {
                TextButton(onClick = { tastingToDelete = null; viewModel.deleteTasting(item) }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { tastingToDelete = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Header(product: ProductWithDetails, average: Int?, count: Int, topPadding: androidx.compose.ui.unit.Dp) {
    val p = product.product
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth()) {
        if (p.photoPath != null) {
            AsyncImage(
                model = File(p.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )
            // Dégradé vers le fond pour fondre la photo dans la page
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to background)),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = if (p.photoPath != null) 200.dp else topPadding + 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                TypeBadge(p.type)
                Text(p.name, style = MaterialTheme.typography.headlineMedium)
                val origin = listOfNotNull(p.producer, p.region, p.country).joinToString(" · ")
                if (origin.isNotEmpty()) {
                    Text(origin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (average != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassGauge(average, p.type, size = 56.dp)
                    Text(RatingScale.format(average, p.type), style = RatingTextStyle, color = p.type.accent())
                    Text(
                        if (count > 1) "moyenne de $count" else "1 dégustation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsBlock(product: ProductWithDetails) {
    val p = product.product
    val facts = buildList {
        p.abv?.let { add("${formatNumber(it)} %") }
        product.beer?.let { b ->
            b.style?.let(::add)
            b.color?.let(::add)
            b.ibu?.let { add("$it IBU") }
        }
        product.whisky?.let { w ->
            w.category?.let(::add)
            w.age?.let { add("$it ans") }
            w.caskType?.let { add("Fût $it") }
            when (w.peat) {
                PeatLevel.NONE -> Unit
                PeatLevel.LIGHT -> add("Légèrement tourbé")
                PeatLevel.MEDIUM -> add("Tourbé")
                PeatLevel.HEAVY -> add("Très tourbé")
            }
            if (w.caskStrength) add("Brut de fût")
        }
    }
    if (facts.isEmpty()) return
    FlowRow(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.forEach { AromaTag(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CellarBlock(
    type: ProductType,
    cellar: CellarItemEntity?,
    onStatus: (CellarStatus?) -> Unit,
    onFillLevel: (Int) -> Unit,
    onEdit: () -> Unit,
) {
    Section("Cave") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val info = cellar?.let { c ->
                listOfNotNull(
                    "${c.quantity} bouteille" + if (c.quantity > 1) "s" else "",
                    c.purchasePrice?.let { "${formatNumber(it)} €" },
                    c.shop,
                ).joinToString(" · ")
            }
            Text(
                info ?: "Pas en cave",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEdit) { Text(if (cellar == null) "Ajouter" else "Modifier") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                CellarStatus.IN_STOCK to "En stock",
                CellarStatus.OPENED to if (type == ProductType.WHISKY) "Entamée" else "Ouverte",
                CellarStatus.EMPTY to "Finie",
                CellarStatus.WISHLIST to "À acheter",
            ).forEach { (status, label) ->
                FilterChip(
                    selected = cellar?.status == status,
                    onClick = { onStatus(if (cellar?.status == status) null else status) },
                    label = { Text(label) },
                )
            }
        }
        if (type == ProductType.WHISKY && cellar != null && cellar.status == CellarStatus.OPENED) {
            // Valeur locale pendant le glisser, enregistrée au relâchement
            var level by remember(cellar.fillLevel) { mutableFloatStateOf((cellar.fillLevel ?: 75).toFloat()) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Niveau", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = level,
                    onValueChange = { level = it },
                    onValueChangeFinished = { onFillLevel(level.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.weight(1f),
                )
                Text("${level.toInt()} %", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TastingItem(item: TastingWithProduct, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val t = item.tasting
    val type = item.product.type
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        RatingScale.format(t.rating, type) + " · " + RatingScale.label(t.rating),
                        style = MaterialTheme.typography.titleMedium,
                        color = type.accent(),
                    )
                    val meta = listOfNotNull(
                        DateFormat.getDateInstance(DateFormat.LONG).format(Date(t.date)),
                        t.place,
                        t.servedAs,
                    ).joinToString(" · ")
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Supprimer la dégustation")
                }
            }
            if (t.photoPath != null && t.photoPath != item.product.photoPath) {
                AsyncImage(
                    model = File(t.photoPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            listOf("Nez" to t.nose, "Bouche" to t.palate, "Finale" to t.finish, "Commentaire" to t.comment)
                .forEach { (label, value) ->
                    if (!value.isNullOrBlank()) {
                        Text(
                            "$label — $value",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            if (item.aromas.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.aromas.forEach { AromaTag(it.name) }
                }
            }
        }
    }
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')
