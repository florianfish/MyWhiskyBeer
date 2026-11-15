package fr.florianfish.mywhiskybeer.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.ui.components.FormTextField
import fr.florianfish.mywhiskybeer.ui.components.PhotoSection
import fr.florianfish.mywhiskybeer.ui.components.ProductDetailsFields
import fr.florianfish.mywhiskybeer.ui.components.TypeBadge

/** Modification des informations d'un produit. Le type (bière / whisky) n'est pas modifiable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onClose: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.saved.collect { onClose() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le produit") },
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
        if (state.original == null) return@Scaffold
        val form = state.form
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeBadge(form.type)
            FormTextField(
                value = form.name,
                onValueChange = { v -> viewModel.updateForm { it.copy(name = v) } },
                label = "Nom",
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                value = form.producer,
                onValueChange = { v -> viewModel.updateForm { it.copy(producer = v) } },
                label = if (form.type == ProductType.BEER) "Brasserie" else "Distillerie",
                capitalization = KeyboardCapitalization.Words,
            )
            ProductDetailsFields(form, viewModel::updateForm)

            Text("Photo", style = MaterialTheme.typography.titleLarge)
            PhotoSection(
                photoPath = state.photoPath,
                loading = state.photoLoading,
                newCaptureUri = viewModel::newCaptureUri,
                onPicked = viewModel::onPhotoPicked,
                onCaptured = viewModel::onPhotoCaptured,
                onRemove = viewModel::removePhoto,
            )

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enregistrer les modifications")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
