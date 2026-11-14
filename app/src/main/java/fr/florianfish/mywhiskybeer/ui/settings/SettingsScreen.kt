package fr.florianfish.mywhiskybeer.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.florianfish.mywhiskybeer.BuildConfig
import fr.florianfish.mywhiskybeer.domain.AppVersion
import fr.florianfish.mywhiskybeer.ui.theme.RatingTextStyle

/** Réglages : sauvegarde Google Drive et version. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val version = AppVersion.parse(BuildConfig.VERSION_NAME, BuildConfig.DEBUG, BuildConfig.COMMIT)
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
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
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackupCard(
                state = state,
                suggestedFileName = viewModel::suggestedFileName,
                onBackup = viewModel::backup,
                onRestorePicked = viewModel::prepareRestore,
                onDismissMessage = viewModel::dismissMessage,
            )

            SettingsCard("À propos") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("MyWhiskyBeer", style = MaterialTheme.typography.titleLarge)
                        Text(
                            version.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (version.isDebug) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ) {
                            Text("DEBUG", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(version.name, style = RatingTextStyle, color = MaterialTheme.colorScheme.primary)
                InfoLine("Code de version", BuildConfig.VERSION_CODE.toString())
                InfoLine("Commit", version.commit)
                InfoLine("Identifiant", BuildConfig.APPLICATION_ID)
            }
        }
    }

    state.pendingRestore?.let { pending ->
        RestoreDialog(pending, onConfirm = viewModel::confirmRestore, onDismiss = viewModel::cancelRestore)
    }
}

@Composable
internal fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BackupCard(
    state: SettingsState,
    suggestedFileName: () -> String,
    onBackup: (Uri) -> Unit,
    onRestorePicked: (Uri) -> Unit,
    onDismissMessage: () -> Unit,
) {
    // Sélecteur de fichiers d'Android : choisir « Drive » pour enregistrer sur Google Drive
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onBackup(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onRestorePicked(uri)
    }

    SettingsCard("Sauvegarde") {
        Text(
            "Toutes tes notes, ta cave et tes photos dans un fichier .zip. " +
                "Dans le sélecteur, choisis Drive pour l'enregistrer sur Google Drive.",
            style = MaterialTheme.typography.bodyMedium,
        )
        val last = state.lastBackupAt
        Text(
            if (last != null) {
                "Dernière sauvegarde : " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(last))
            } else {
                "Aucune sauvegarde depuis cet appareil"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (last == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
            Button(
                onClick = { backupLauncher.launch(suggestedFileName()) },
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Sauvegarder")
            }
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Restaurer")
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissMessage) { Text("OK") }
            }
        }
    }
}

@Composable
private fun RestoreDialog(pending: PendingRestore, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restaurer cette sauvegarde ?") },
        text = {
            Text(
                "Elle contient ${pending.summary.text}.\n\n" +
                    "Toutes les données actuelles de l'appli (notes, cave, photos) seront remplacées. " +
                    "Pense à sauvegarder d'abord si tu veux les garder.",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Remplacer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
