package fr.florianfish.mywhiskybeer.data.barcode

import fr.florianfish.mywhiskybeer.BuildConfig
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.settings.AppSettings
import fr.florianfish.mywhiskybeer.domain.Barcodes
import fr.florianfish.mywhiskybeer.domain.LabelGuess
import fr.florianfish.mywhiskybeer.domain.OpenFoodFactsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Résultat de la recherche d'un code-barres. */
sealed interface BarcodeResult {
    /** Déjà dans le carnet : fonctionne hors-ligne. */
    data class Existing(val product: ProductWithDetails) : BarcodeResult
    /** Trouvé sur Open Food Facts. */
    data class Found(val guess: LabelGuess) : BarcodeResult
    data object NotFound : BarcodeResult
    /** Recherche en ligne désactivée dans les réglages. */
    data object Disabled : BarcodeResult
    /** Pas de réseau, ou Open Food Facts indisponible. */
    data object Unreachable : BarcodeResult
    /** Code mal lu (clé de contrôle fausse). */
    data object Invalid : BarcodeResult
}

/**
 * Cherche d'abord le code dans le carnet (hors-ligne), puis, si l'utilisateur l'a permis,
 * sur Open Food Facts : c'est le seul accès à internet de l'appli.
 */
@Singleton
class BarcodeLookup @Inject constructor(
    private val productDao: ProductDao,
    private val settings: AppSettings,
) {
    suspend fun lookup(code: String): BarcodeResult {
        productDao.findByBarcode(code)?.let { found ->
            productDao.observeById(found.id).first()?.let { return BarcodeResult.Existing(it) }
        }
        if (!Barcodes.isValid(code)) return BarcodeResult.Invalid
        if (!settings.openFoodFactsEnabled.value) return BarcodeResult.Disabled
        return fetchOpenFoodFacts(code)
    }

    private suspend fun fetchOpenFoodFacts(code: String): BarcodeResult = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=${OpenFoodFactsParser.FIELDS}")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                // Open Food Facts demande un User-Agent identifiant l'appli
                setRequestProperty("User-Agent", "MyWhiskyBeer/${BuildConfig.VERSION_NAME} (https://github.com/florianfish/MyWhiskyBeer)")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val status = connection.responseCode
                // Produit inconnu : l'API répond 404 avec status = 0
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                when {
                    status == 404 -> BarcodeResult.NotFound
                    status !in 200..299 -> BarcodeResult.Unreachable
                    else -> OpenFoodFactsParser.parse(body)?.let { BarcodeResult.Found(it) } ?: BarcodeResult.NotFound
                }
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(BarcodeResult.Unreachable)
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
    }
}

/** Message affiché sous le formulaire après un scan. */
fun BarcodeResult.message(code: String): String = when (this) {
    is BarcodeResult.Existing -> "Déjà dans ton carnet (code $code)"
    is BarcodeResult.Found -> "Complété avec Open Food Facts : vérifie les champs"
    BarcodeResult.NotFound -> "Code $code inconnu d'Open Food Facts : complète à la main, il sera reconnu au prochain scan"
    BarcodeResult.Disabled -> "Code $code enregistré (recherche Open Food Facts désactivée dans Réglages)"
    BarcodeResult.Unreachable -> "Open Food Facts injoignable : code $code enregistré, complète à la main"
    BarcodeResult.Invalid -> "Code mal lu, réessaie"
}
