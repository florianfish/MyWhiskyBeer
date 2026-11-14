package fr.florianfish.mywhiskybeer.domain

/**
 * Version de l'appli, lue depuis versionName.
 * Release : « 1.4.0 ». Debug : « 1.4.0-debug.57 » = compilé après la release 1.4.0, build CI n° 57
 * (« 0.1.0-debug » pour un build local, sans numéro).
 */
data class AppVersion(
    val name: String,
    /** Version de release de référence (« 1.4.0 »). */
    val release: String,
    val isDebug: Boolean,
    /** Numéro du build CI, null pour un build local ou une release. */
    val debugRun: Int?,
    val commit: String,
) {
    /** Explication en une ligne, pour l'écran À propos. */
    val description: String
        get() = when {
            !isDebug -> "Version officielle"
            debugRun != null -> "Build de test n° $debugRun, compilé après la version $release"
            else -> "Build de test local, basé sur la version $release"
        }

    companion object {
        private val debugSuffix = Regex("""-debug(?:\.(\d+))?$""")

        fun parse(versionName: String, isDebugBuild: Boolean, commit: String): AppVersion {
            val match = debugSuffix.find(versionName)
            return AppVersion(
                name = versionName,
                release = if (match != null) versionName.substring(0, match.range.first) else versionName,
                isDebug = isDebugBuild || match != null,
                debugRun = match?.groupValues?.get(1)?.toIntOrNull(),
                commit = commit,
            )
        }
    }
}
