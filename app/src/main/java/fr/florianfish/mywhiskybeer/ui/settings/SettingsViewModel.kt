package fr.florianfish.mywhiskybeer.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.backup.BackupManager
import fr.florianfish.mywhiskybeer.data.backup.BackupSummary
import fr.florianfish.mywhiskybeer.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val lastBackupAt: Long? = null,
    val busy: Boolean = false,
    /** Message à afficher après une opération (succès ou erreur). */
    val message: String? = null,
    val error: Boolean = false,
    /** Sauvegarde choisie, en attente de confirmation avant de remplacer les données. */
    val pendingRestore: PendingRestore? = null,
)

data class PendingRestore(val uri: Uri, val summary: BackupSummary)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val appSettings: AppSettings,
) : ViewModel() {

    /** Recherche des codes-barres inconnus sur Open Food Facts. */
    val openFoodFacts: StateFlow<Boolean> = appSettings.openFoodFactsEnabled

    fun setOpenFoodFacts(enabled: Boolean) = appSettings.setOpenFoodFactsEnabled(enabled)

    private val _state = MutableStateFlow(SettingsState(lastBackupAt = backupManager.lastBackupAt))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun suggestedFileName(): String = backupManager.suggestedFileName()

    fun backup(target: Uri) = run("Échec de la sauvegarde") {
        val summary = backupManager.backup(target)
        _state.update { it.copy(lastBackupAt = backupManager.lastBackupAt, message = "Sauvegarde enregistrée : ${summary.text}.") }
    }

    /** Étape 1 : lire la sauvegarde choisie et demander confirmation. */
    fun prepareRestore(source: Uri) = run("Fichier de sauvegarde illisible") {
        val summary = backupManager.inspect(source)
        _state.update { it.copy(pendingRestore = PendingRestore(source, summary)) }
    }

    /** Étape 2 : remplacer les données. */
    fun confirmRestore() {
        val pending = _state.value.pendingRestore ?: return
        _state.update { it.copy(pendingRestore = null) }
        run("Échec de la restauration, tes données n'ont pas été modifiées") {
            val summary = backupManager.restore(pending.uri)
            _state.update { it.copy(message = "Sauvegarde restaurée : ${summary.text}.") }
        }
    }

    fun cancelRestore() = _state.update { it.copy(pendingRestore = null) }

    fun dismissMessage() = _state.update { it.copy(message = null, error = false) }

    private fun run(errorPrefix: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null, error = false) }
            try {
                block()
            } catch (e: Exception) {
                val detail = e.message?.let { " ($it)" }.orEmpty()
                _state.update { it.copy(message = "$errorPrefix$detail.", error = true) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
