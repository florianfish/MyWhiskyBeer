package fr.florianfish.mywhiskybeer.data.place

import fr.florianfish.mywhiskybeer.BuildConfig
import fr.florianfish.mywhiskybeer.data.settings.AppSettings
import fr.florianfish.mywhiskybeer.domain.PlaceSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recherche de villes du monde entier avec Photon (OpenStreetMap, service public sans clé,
 * usage raisonnable demandé, sans garantie de disponibilité). Désactivable dans les réglages.
 * En cas d'erreur ou hors-ligne : liste vide, le champ reste en saisie libre.
 */
@Singleton
class CitySearch @Inject constructor(
    private val settings: AppSettings,
) {
    suspend fun search(query: String): List<String> {
        val q = query.trim()
        if (q.length < PlaceSuggestions.ONLINE_MIN_LENGTH || !settings.citySearchEnabled.value) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val url = URL("https://photon.komoot.io/api/?q=$encoded&layer=city&limit=${PlaceSuggestions.MAX}&lang=fr")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("User-Agent", "MyWhiskyBeer/${BuildConfig.VERSION_NAME} (https://github.com/florianfish/MyWhiskyBeer)")
                }
                try {
                    if (connection.responseCode !in 200..299) return@runCatching emptyList()
                    PlaceSuggestions.parsePhoton(connection.inputStream.bufferedReader().use { it.readText() })
                } finally {
                    connection.disconnect()
                }
            }.getOrDefault(emptyList())
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000
    }
}
