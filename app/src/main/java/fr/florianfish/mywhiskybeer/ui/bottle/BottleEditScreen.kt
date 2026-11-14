package fr.florianfish.mywhiskybeer.ui.bottle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.ui.components.ExistingProductCard
import fr.florianfish.mywhiskybeer.ui.components.FormTextField
import fr.florianfish.mywhiskybeer.ui.components.PhotoSection
import fr.florianfish.mywhiskybeer.ui.components.ProductFormSection
import fr.florianfish.mywhiskybeer.ui.theme.RatingTextStyle
import fr.florianfish.mywhiskybeer.ui.theme.accent
import java.text.DateFormat
import java.util.Date

/** Ajout (ou modification) d'une bouteille en cave : c'est du stock, pas une dégustation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottleEditScreen(
    onClose: () -> Unit,
    viewModel: BottleEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.saved.collect { onClose() } }

    val title = when {
        state.editing -> "Modifier la bouteille"
        state.stock.status == CellarStatus.WISHLIST -> "À acheter"
        else -> "Ajouter une bouteille"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                ExistingProductCard(existing, onChange = if (state.productLocked) null else viewModel::clearExisting)
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
                )
            }

            // Photo : seulement pour un nouveau produit, ou un produit qui n'en a pas encore
            if (existing == null || existing.product.photoPath == null) {
                PhotoSection(
                    photoPath = state.photoPath,
                    loading = state.photoLoading,
                    newCaptureUri = viewModel::newCaptureUri,
                    onPicked = viewModel::onPhotoPicked,
                    onCaptured = viewModel::onPhotoCaptured,
                    onRemove = viewModel::removePhoto,
                )
            }

            StockSection(
                stock = state.stock,
                type = state.type,
                onStatus = viewModel::setStatus,
                onChange = viewModel::updateStock,
            )

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        when {
                            state.editing -> "Enregistrer"
                            state.stock.status == CellarStatus.WISHLIST -> "Ajouter à la liste d'achats"
                            else -> "Ajouter à la cave"
                        },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockSection(
    stock: StockForm,
    type: ProductType,
    onStatus: (CellarStatus) -> Unit,
    onChange: ((StockForm) -> StockForm) -> Unit,
) {
    val wishlist = stock.status == CellarStatus.WISHLIST
    var pickDate by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Stock", style = MaterialTheme.typography.titleLarge)

        val statuses = listOf(
            CellarStatus.IN_STOCK to "En stock",
            CellarStatus.OPENED to if (type == ProductType.WHISKY) "Entamée" else "Ouverte",
            CellarStatus.WISHLIST to "À acheter",
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            statuses.forEachIndexed { i, (status, label) ->
                SegmentedButton(
                    selected = stock.status == status,
                    onClick = { onStatus(status) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = statuses.size),
                ) { Text(label) }
            }
        }

        // Nombre de bouteilles
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (wishlist) "Bouteilles à acheter" else "Bouteilles",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(
                onClick = { onChange { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) } },
                enabled = stock.quantity > 1,
            ) { Icon(Icons.Outlined.Remove, contentDescription = "Une de moins") }
            Text(
                stock.quantity.toString(),
                style = RatingTextStyle,
                color = type.accent(),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 48.dp),
            )
            FilledTonalIconButton(onClick = { onChange { it.copy(quantity = it.quantity + 1) } }) {
                Icon(Icons.Outlined.Add, contentDescription = "Une de plus")
            }
        }

        if (stock.status == CellarStatus.OPENED) {
            Column {
                Text("Niveau restant : ${stock.fillLevel} %", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = stock.fillLevel.toFloat(),
                    onValueChange = { v -> onChange { it.copy(fillLevel = v.toInt()) } },
                    valueRange = 5f..95f,
                    steps = 17,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                stock.price, { v -> onChange { it.copy(price = v) } },
                if (wishlist) "Prix repéré (€)" else "Prix d'achat (€)",
                Modifier.weight(1f), KeyboardType.Decimal,
            )
            FormTextField(
                stock.shop, { v -> onChange { it.copy(shop = v) } },
                if (wishlist) "Où l'acheter" else "Caviste / magasin",
                Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Words,
            )
        }

        if (!wishlist) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val date = stock.purchaseDate
                Text(
                    if (date != null) "Achetée le " + DateFormat.getDateInstance(DateFormat.LONG).format(Date(date)) else "Date d'achat inconnue",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { pickDate = true }) { Text("Changer") }
            }
        }
    }

    if (pickDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = stock.purchaseDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickDate = false
                    onChange { it.copy(purchaseDate = pickerState.selectedDateMillis) }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Annuler") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
