package fr.florianfish.mywhiskybeer.ui.components

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import fr.florianfish.mywhiskybeer.data.local.entity.PeatLevel
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.domain.ProductForm
import fr.florianfish.mywhiskybeer.ui.theme.accent
import java.io.File

/** Rappel du produit déjà en base, avec un bouton pour en choisir un autre. */
@Composable
fun ExistingProductCard(product: ProductWithDetails, onChange: (() -> Unit)?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TypeBadge(product.product.type)
                Text(product.product.name, style = MaterialTheme.typography.titleLarge)
                product.product.producer?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onChange != null) TextButton(onClick = onChange) { Text("Changer") }
        }
    }
}

/**
 * Saisie d'un nouveau produit : scan de l'étiquette, type, nom (avec suggestions anti-doublon),
 * producteur et détails repliables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormSection(
    form: ProductForm,
    suggestions: List<ProductWithDetails>,
    scanning: Boolean,
    scannedLines: List<String>,
    newCaptureUri: () -> Uri,
    onLabelCaptured: () -> Unit,
    onFormChange: ((ProductForm) -> ProductForm) -> Unit,
    onSuggestionClick: (ProductWithDetails) -> Unit,
    barcodeBusy: Boolean,
    onBarcode: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var scannerError by remember { mutableStateOf<String?>(null) }
    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) onLabelCaptured()
    }
    // L'étiquette a rempli des détails : on les montre
    LaunchedEffect(scannedLines) {
        if (scannedLines.isNotEmpty() && form.hasDetails()) expanded = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = { scanLauncher.launch(newCaptureUri()) },
                enabled = !scanning && !barcodeBusy,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                if (scanning) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.DocumentScanner, contentDescription = null)
                }
                Spacer(Modifier.size(8.dp))
                Text("Étiquette")
            }
            FilledTonalButton(
                onClick = {
                    scannerError = null
                    startBarcodeScan(context, onBarcode) { scannerError = it }
                },
                enabled = !scanning && !barcodeBusy,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                if (barcodeBusy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                }
                Spacer(Modifier.size(8.dp))
                Text("Code-barres")
            }
        }
        scannerError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        if (scannedLines.isNotEmpty()) {
            ScannedLines(scannedLines, onFormChange)
        }

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

        FormTextField(
            value = form.name,
            onValueChange = { v -> onFormChange { it.copy(name = v) } },
            label = "Nom",
            capitalization = KeyboardCapitalization.Words,
        )
        if (suggestions.isNotEmpty()) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
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
        FormTextField(
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

/** Texte lu sur l'étiquette : un appui sur une ligne permet de l'utiliser comme nom ou producteur. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScannedLines(lines: List<String>, onFormChange: ((ProductForm) -> ProductForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Lu sur l'étiquette — touche une ligne pour l'utiliser",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.distinct().take(12).forEach { line ->
                var menu by remember(line) { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { menu = true },
                        label = { Text(line, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Comme nom") },
                            onClick = { menu = false; onFormChange { it.copy(name = line.trim()) } },
                        )
                        DropdownMenuItem(
                            text = { Text("Comme producteur") },
                            onClick = { menu = false; onFormChange { it.copy(producer = line.trim()) } },
                        )
                        DropdownMenuItem(
                            text = { Text("Ajouter au nom") },
                            onClick = {
                                menu = false
                                onFormChange { it.copy(name = listOf(it.name.trim(), line.trim()).filter(String::isNotEmpty).joinToString(" ")) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProductDetailsFields(form: ProductForm, onFormChange: ((ProductForm) -> ProductForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormTextField(form.barcode, { v -> onFormChange { it.copy(barcode = v.filter(Char::isDigit)) } }, "Code-barres", keyboardType = KeyboardType.Number)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(form.country, { v -> onFormChange { it.copy(country = v) } }, "Pays", Modifier.weight(1f))
            FormTextField(form.region, { v -> onFormChange { it.copy(region = v) } }, "Région", Modifier.weight(1f))
        }
        when (form.type) {
            ProductType.BEER -> {
                FormTextField(form.style, { v -> onFormChange { it.copy(style = v) } }, "Style (IPA, stout…)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormTextField(form.abv, { v -> onFormChange { it.copy(abv = v) } }, "Degré %", Modifier.weight(1f), KeyboardType.Decimal)
                    FormTextField(form.ibu, { v -> onFormChange { it.copy(ibu = v) } }, "IBU", Modifier.weight(1f), KeyboardType.Number)
                }
                FormTextField(form.color, { v -> onFormChange { it.copy(color = v) } }, "Couleur (blonde, ambrée…)")
            }
            ProductType.WHISKY -> {
                FormTextField(form.category, { v -> onFormChange { it.copy(category = v) } }, "Catégorie (single malt, bourbon…)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormTextField(form.age, { v -> onFormChange { it.copy(age = v) } }, "Âge (ans)", Modifier.weight(1f), KeyboardType.Number)
                    FormTextField(form.abv, { v -> onFormChange { it.copy(abv = v) } }, "Degré %", Modifier.weight(1f), KeyboardType.Decimal)
                }
                FormTextField(form.caskType, { v -> onFormChange { it.copy(caskType = v) } }, "Fût (ex-bourbon, sherry…)")
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

/** Photo (aperçu + boutons appareil photo / galerie). */
@Composable
fun PhotoSection(
    photoPath: String?,
    loading: Boolean,
    newCaptureUri: () -> Uri,
    onPicked: (Uri) -> Unit,
    onCaptured: () -> Unit,
    onRemove: () -> Unit,
) {
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) onCaptured()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        contentDescription = "Photo",
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
}

@Composable
fun FormTextField(
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

/**
 * Ouvre le scanner de codes-barres de Google Play services (interface fournie, pas de permission caméra).
 * Formats des bouteilles et canettes : EAN-13, EAN-8, UPC-A, UPC-E.
 */
private fun startBarcodeScan(context: Context, onBarcode: (String) -> Unit, onError: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.takeIf { it.isNotBlank() }?.let(onBarcode) }
        .addOnFailureListener { onError("Scanner indisponible (services Google Play) : ${it.message ?: "erreur inconnue"}") }
}
