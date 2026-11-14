package fr.florianfish.mywhiskybeer.ui.tasting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.PeatLevel
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.ui.components.AromaWheel
import fr.florianfish.mywhiskybeer.ui.components.PourSlider
import fr.florianfish.mywhiskybeer.ui.components.TypeBadge
import fr.florianfish.mywhiskybeer.ui.theme.accent
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TastingEditScreen(
    onClose: () -> Unit,
    onSaved: (productId: Long) -> Unit,
    viewModel: TastingEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val aromas by viewModel.aromas.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle dégustation") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Fermer") }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = state.canSave) { Text("Enregistrer") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val existing = state.existing
            if (existing != null) {
                ExistingProductCard(existing, onChange = viewModel::clearExisting)
            } else {
                ProductSection(
                    form = state.product,
                    suggestions = suggestions,
                    onFormChange = viewModel::updateProduct,
                    onSuggestionClick = viewModel::selectExisting,
                )
            }

            PhotoSection(
                photoPath = state.tasting.photoPath,
                loading = state.photoLoading,
                newCaptureUri = viewModel::newCaptureUri,
                onPicked = viewModel::onPhotoPicked,
                onCaptured = viewModel::onPhotoCaptured,
                onRemove = viewModel::removePhoto,
            )

            SectionTitle("Note")
            PourSlider(
                rating = state.tasting.rating,
                type = state.type,
                onRatingChange = { r -> viewModel.updateTasting { it.copy(rating = r) } },
            )

            SectionTitle("Arômes")
            Text(
                "Touche un arôme pour le cocher, retouche pour l'intensifier (léger, marqué, dominant).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AromaWheel(aromas = aromas, selected = state.tasting.aromas, onAromaTap = viewModel::cycleAroma)
            SelectedAromas(aromas, state.tasting.aromas, onTap = viewModel::cycleAroma)

            SectionTitle("Notes de dégustation")
            TastingNotes(state.tasting, state.type) { transform -> viewModel.updateTasting(transform) }

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enregistrer la dégustation")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ExistingProductCard(product: ProductWithDetails, onChange: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TypeBadge(product.product.type)
                Text(product.product.name, style = MaterialTheme.typography.titleLarge)
                product.product.producer?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onChange) { Text("Changer") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSection(
    form: ProductForm,
    suggestions: List<ProductWithDetails>,
    onFormChange: ((ProductForm) -> ProductForm) -> Unit,
    onSuggestionClick: (ProductWithDetails) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(ProductType.WHISKY to "Whisky", ProductType.BEER to "Bière").forEachIndexed { i, (type, label) ->
                SegmentedButton(
                    selected = form.type == type,
                    onClick = { onFormChange { it.copy(type = type) } },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = type.accent().copy(alpha = 0.25f)),
                ) { Text(label) }
            }
        }

        TextInput(
            value = form.name,
            onValueChange = { v -> onFormChange { it.copy(name = v) } },
            label = "Nom",
            capitalization = KeyboardCapitalization.Words,
        )
        if (suggestions.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column {
                    Text(
                        "Déjà dans ton carnet ?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                    )
                    suggestions.forEach { s ->
                        val producer = s.product.producer
                        ListItem(
                            headlineContent = { Text(s.product.name) },
                            supportingContent = if (producer != null) { { Text(producer) } } else null,
                            overlineContent = { TypeBadge(s.product.type) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.clickable { onSuggestionClick(s) },
                        )
                    }
                }
            }
        }
        TextInput(
            value = form.producer,
            onValueChange = { v -> onFormChange { it.copy(producer = v) } },
            label = if (form.type == ProductType.BEER) "Brasserie" else "Distillerie",
            capitalization = KeyboardCapitalization.Words,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Plus de détails", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(expanded) {
            ProductDetailsFields(form, onFormChange)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailsFields(form: ProductForm, onFormChange: ((ProductForm) -> ProductForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextInput(form.country, { v -> onFormChange { it.copy(country = v) } }, "Pays", Modifier.weight(1f))
            TextInput(form.region, { v -> onFormChange { it.copy(region = v) } }, "Région", Modifier.weight(1f))
        }
        when (form.type) {
            ProductType.BEER -> {
                TextInput(form.style, { v -> onFormChange { it.copy(style = v) } }, "Style (IPA, stout…)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextInput(form.abv, { v -> onFormChange { it.copy(abv = v) } }, "Degré %", Modifier.weight(1f), KeyboardType.Decimal)
                    TextInput(form.ibu, { v -> onFormChange { it.copy(ibu = v) } }, "IBU", Modifier.weight(1f), KeyboardType.Number)
                }
                TextInput(form.color, { v -> onFormChange { it.copy(color = v) } }, "Couleur (blonde, ambrée…)")
            }
            ProductType.WHISKY -> {
                TextInput(form.category, { v -> onFormChange { it.copy(category = v) } }, "Catégorie (single malt, bourbon…)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextInput(form.age, { v -> onFormChange { it.copy(age = v) } }, "Âge (ans)", Modifier.weight(1f), KeyboardType.Number)
                    TextInput(form.abv, { v -> onFormChange { it.copy(abv = v) } }, "Degré %", Modifier.weight(1f), KeyboardType.Decimal)
                }
                TextInput(form.caskType, { v -> onFormChange { it.copy(caskType = v) } }, "Fût (ex-bourbon, sherry…)")
                Text("Tourbe", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        PeatLevel.NONE to "Aucune",
                        PeatLevel.LIGHT to "Légère",
                        PeatLevel.MEDIUM to "Moyenne",
                        PeatLevel.HEAVY to "Forte",
                    ).forEach { (level, label) ->
                        FilterChip(
                            selected = form.peat == level,
                            onClick = { onFormChange { it.copy(peat = level) } },
                            label = { Text(label) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brut de fût (cask strength)", modifier = Modifier.weight(1f))
                    Switch(checked = form.caskStrength, onCheckedChange = { c -> onFormChange { it.copy(caskStrength = c) } })
                }
            }
        }
    }
}

@Composable
private fun PhotoSection(
    photoPath: String?,
    loading: Boolean,
    newCaptureUri: () -> android.net.Uri,
    onPicked: (android.net.Uri) -> Unit,
    onCaptured: () -> Unit,
    onRemove: () -> Unit,
) {
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) onCaptured()
    }

    if (photoPath != null || loading) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            if (photoPath != null) {
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = "Photo de la dégustation",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, contentDescription = "Retirer la photo") }
                }
            }
            if (loading) CircularProgressIndicator()
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { captureLauncher.launch(newCaptureUri()) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Photo")
        }
        OutlinedButton(
            onClick = { pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Galerie")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedAromas(aromas: List<AromaEntity>, selected: Map<Long, Int>, onTap: (Long) -> Unit) {
    val chosen = aromas.filter { it.id in selected }
    if (chosen.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chosen.forEach { aroma ->
            val intensity = selected[aroma.id] ?: 0
            FilterChip(
                selected = true,
                onClick = { onTap(aroma.id) },
                label = { Text("${aroma.name} " + "●".repeat(intensity) + "○".repeat(3 - intensity)) },
            )
        }
    }
}

@Composable
private fun TastingNotes(form: TastingForm, type: ProductType, onChange: ((TastingForm) -> TastingForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextInput(form.nose, { v -> onChange { it.copy(nose = v) } }, "Nez", singleLine = false)
        TextInput(form.palate, { v -> onChange { it.copy(palate = v) } }, "Bouche", singleLine = false)
        TextInput(form.finish, { v -> onChange { it.copy(finish = v) } }, "Finale", singleLine = false)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        TextInput(form.comment, { v -> onChange { it.copy(comment = v) } }, "Commentaire", singleLine = false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextInput(
                form.place, { v -> onChange { it.copy(place = v) } }, "Lieu", Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Words,
            )
            TextInput(
                form.servedAs, { v -> onChange { it.copy(servedAs = v) } },
                if (type == ProductType.WHISKY) "Service (sec, eau…)" else "Service (pression…)",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        modifier = modifier.fillMaxWidth(),
    )
}
