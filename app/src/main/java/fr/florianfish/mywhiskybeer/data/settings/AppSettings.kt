package fr.florianfish.mywhiskybeer.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Réglages de l'appli (SharedPreferences). */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _openFoodFactsEnabled = MutableStateFlow(prefs.getBoolean(KEY_OFF, true))
    /** Recherche des codes-barres inconnus sur Open Food Facts (seul accès à internet de l'appli). */
    val openFoodFactsEnabled: StateFlow<Boolean> = _openFoodFactsEnabled.asStateFlow()

    fun setOpenFoodFactsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OFF, enabled).apply()
        _openFoodFactsEnabled.value = enabled
    }

    private companion object {
        const val KEY_OFF = "open_food_facts"
    }
}
