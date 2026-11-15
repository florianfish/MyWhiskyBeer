package fr.florianfish.mywhiskybeer.ui.tasting

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.ui.components.AromaWheel
import fr.florianfish.mywhiskybeer.ui.components.ExistingProductCard
import fr.florianfish.mywhiskybeer.ui.components.FormTextField
import fr.florianfish.mywhiskybeer.ui.components.PhotoSection
import fr.florianfish.mywhiskybeer.ui.components.PourSlider
import fr.florianfish.mywhiskybeer.ui.components.ProductFormSection

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
    val producerSuggestions by viewModel.producerSuggestions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing) "Modifier la dégustation" else "Nouvelle dégustation") },
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
                // En modification, la dégustation reste rattachée à son produit
                ExistingProductCard(existing, onChange = if (state.editing) null else viewModel::clearExisting)
            } else {
                ProductFormSection(
                    form = state.product,
                    suggestions = suggestions,
                    scanning = state.scanning,
                    scannedLines = state.scannedLines,
                    newCaptureUri = viewModel::newCaptureUri,
                    onLabelCaptured = viewModel::onLabelCaptured,
                    onFormChange = viewModel::updateProduct,
                    onSuggestionClick = viewModel::selectExisting,
                    barcodeBusy = state.barcodeBusy,
                    onBarcode = viewModel::onBarcodeScanned,
                    producerSuggestions = producerSuggestions,
                    onProducerPick = viewModel::pickProducer,
                )
            }
            state.barcodeMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
                    Text(if (state.editing) "Enregistrer les modifications" else "Enregistrer la dégustation")
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
        FormTextField(form.nose, { v -> onChange { it.copy(nose = v) } }, "Nez", singleLine = false)
        FormTextField(form.palate, { v -> onChange { it.copy(palate = v) } }, "Bouche", singleLine = false)
        FormTextField(form.finish, { v -> onChange { it.copy(finish = v) } }, "Finale", singleLine = false)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        FormTextField(form.comment, { v -> onChange { it.copy(comment = v) } }, "Commentaire", singleLine = false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                form.place, { v -> onChange { it.copy(place = v) } }, "Lieu", Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                form.servedAs, { v -> onChange { it.copy(servedAs = v) } },
                if (type == ProductType.WHISKY) "Service (sec, eau…)" else "Service (pression…)",
                Modifier.weight(1f),
            )
        }
    }
}
