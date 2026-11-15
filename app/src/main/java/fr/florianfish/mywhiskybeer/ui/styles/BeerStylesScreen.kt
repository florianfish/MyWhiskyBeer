package fr.florianfish.mywhiskybeer.ui.styles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Réglages › Styles de bière : ajouter, retirer (avec remplacement si utilisé), remettre un style par défaut. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BeerStylesScreen(onBack: () -> Unit, viewModel: BeerStylesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var newStyle by remember { mutableStateOf("") }
    var toRemove by remember { mutableStateOf<StyleRow?>(null) }

    fun addNew() {
        if (newStyle.isNotBlank()) viewModel.add(newStyle)
        newStyle = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Styles de bière") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newStyle,
                        onValueChange = { newStyle = it },
                        label = { Text("Nouveau style") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addNew() }),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { addNew() }, enabled = newStyle.isNotBlank()) {
                        Icon(Icons.Outlined.Add, contentDescription = "Ajouter")
                    }
                }
            }
            if (state.removedDefaults.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Styles par défaut retirés", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.removedDefaults.forEach { s ->
                                AssistChip(
                                    onClick = { viewModel.add(s) },
                                    label = { Text(s) },
                                    leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "${state.styles.size} styles",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            items(state.styles, key = { it.name }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            when (row.count) {
                                0 -> "Non utilisé"
                                1 -> "1 bière"
                                else -> "${row.count} bières"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { toRemove = row }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Retirer ${row.name}")
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    toRemove?.let { row ->
        RemoveDialog(
            row = row,
            others = state.styles.map { it.name }.filter { it != row.name },
            onConfirm = { replacement ->
                viewModel.remove(row.name, replacement)
                toRemove = null
            },
            onDismiss = { toRemove = null },
        )
    }
}

/**
 * Confirmation du retrait. Un style utilisé ne peut pas être retiré sans choisir
 * le style qui le remplace dans les bières concernées.
 */
@Composable
private fun RemoveDialog(row: StyleRow, others: List<String>, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var replacement by remember { mutableStateOf<String?>(null) }
    val used = row.count > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirer « ${row.name} » ?") },
        text = {
            if (!used) {
                Text("Aucune bière n'utilise ce style.")
            } else {
                Column {
                    Text(
                        (if (row.count > 1) "${row.count} bières utilisent" else "1 bière utilise") +
                            " ce style. Choisis le style qui le remplace :",
                    )
                    Column(Modifier.padding(top = 8.dp).verticalScroll(rememberScrollState())) {
                        others.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().clickable { replacement = s },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = replacement == s, onClick = { replacement = s })
                                Text(s)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(replacement) }, enabled = !used || replacement != null) {
                Text(if (used) "Remplacer et retirer" else "Retirer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
