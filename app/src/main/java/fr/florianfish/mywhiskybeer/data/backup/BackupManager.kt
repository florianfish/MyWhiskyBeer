package fr.florianfish.mywhiskybeer.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.florianfish.mywhiskybeer.BuildConfig
import fr.florianfish.mywhiskybeer.data.local.AppDatabase
import fr.florianfish.mywhiskybeer.data.local.dao.BackupDao
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.TastingAromaCrossRef
import fr.florianfish.mywhiskybeer.data.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sauvegarde complète (notes + photos) dans un fichier .zip choisi par l'utilisateur
 * via le sélecteur de fichiers d'Android : Google Drive y apparaît comme destination.
 * Pas de compte Google à configurer dans l'appli, pas de clé d'API.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val dao: BackupDao,
    private val photoStore: PhotoStore,
) {
    private val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)

    /** Date de la dernière sauvegarde réussie (millisecondes), null si jamais. */
    val lastBackupAt: Long?
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0 }

    suspend fun backup(target: Uri): BackupSummary = withContext(Dispatchers.IO) {
        // Lecture cohérente de toute la base
        val data = db.withTransaction {
            val aromasById = dao.aromas().associateBy { it.id }
            BackupData(
                createdAt = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
                products = dao.products(),
                beerDetails = dao.beerDetails(),
                whiskyDetails = dao.whiskyDetails(),
                tastings = dao.tastings(),
                aromas = dao.tastingAromas().mapNotNull { ref ->
                    aromasById[ref.aromaId]?.let { a -> AromaRef(ref.tastingId, a.productType, a.family, a.name, ref.intensity) }
                },
                cellar = dao.cellar(),
                beerStyles = dao.beerStyles(),
            )
        }
        // Chemins absolus → noms de fichiers dans le zip
        val photos = mutableMapOf<String, File>()
        val portable = BackupCodec.mapPhotos(data) { path ->
            val file = File(path)
            if (file.isFile) {
                photos[file.name] = file
                file.name
            } else {
                null // photo disparue : on ne la référence plus
            }
        }

        val output = context.contentResolver.openOutputStream(target, "wt")
            ?: error("Impossible d'écrire dans le fichier choisi")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupCodec.JSON_ENTRY))
            zip.write(BackupCodec.encode(portable).toByteArray())
            zip.closeEntry()
            photos.forEach { (name, file) ->
                zip.putNextEntry(ZipEntry(BackupCodec.PHOTOS_DIR + name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
        BackupCodec.summary(portable)
    }

    /** Lit une sauvegarde sans rien modifier, pour la présenter avant de restaurer. */
    suspend fun inspect(source: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val staging = stagingDir()
        try {
            BackupCodec.summary(readArchive(source, staging))
        } finally {
            staging.deleteRecursively()
        }
    }

    /**
     * Remplace toutes les données par celles de la sauvegarde.
     * Le zip est entièrement lu et vérifié avant de toucher à la base : en cas d'erreur, rien n'est modifié.
     */
    suspend fun restore(source: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val staging = stagingDir()
        try {
            val portable = readArchive(source, staging)
            val photosDir = photoStore.photosDir
            val data = BackupCodec.mapPhotos(portable) { name ->
                if (File(staging, name).isFile) File(photosDir, name).absolutePath else null
            }

            db.withTransaction {
                dao.clearTastingAromas()
                dao.clearTastings()
                dao.clearCellar()
                dao.clearBeerDetails()
                dao.clearWhiskyDetails()
                dao.clearProducts()
                dao.clearBeerStyles()

                dao.insertProducts(data.products)
                dao.insertBeerDetails(data.beerDetails)
                dao.insertWhiskyDetails(data.whiskyDetails)
                dao.insertTastings(data.tastings)
                dao.insertCellar(data.cellar)
                dao.insertBeerStyles(BackupCodec.beerStyles(data))

                // Arômes : retrouvés par type/famille/nom, ajoutés au référentiel s'ils n'y sont plus
                val known = dao.aromas().associateBy { Triple(it.productType, it.family, it.name) }.toMutableMap()
                val refs = data.aromas.map { ref ->
                    val key = Triple(ref.type, ref.family, ref.name)
                    val id = known[key]?.id ?: dao.insertAroma(AromaEntity(productType = ref.type, family = ref.family, name = ref.name))
                        .also { newId -> known[key] = AromaEntity(newId, ref.type, ref.family, ref.name) }
                    TastingAromaCrossRef(ref.tastingId, id, ref.intensity)
                }.distinctBy { it.tastingId to it.aromaId }
                dao.insertTastingAromas(refs)
            }

            // Photos : on installe celles de la sauvegarde puis on retire celles qui ne servent plus
            staging.listFiles()?.forEach { file -> file.copyTo(File(photosDir, file.name), overwrite = true) }
            val used = (data.products.mapNotNull { it.photoPath } + data.tastings.mapNotNull { it.photoPath })
                .map { File(it).name }.toSet()
            photosDir.listFiles()?.filter { it.name !in used }?.forEach { it.delete() }

            BackupCodec.summary(portable)
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Lit le zip : renvoie les données et dépose les photos dans [staging]. */
    private fun readArchive(source: Uri, staging: File): BackupData {
        var json: String? = null
        val input = context.contentResolver.openInputStream(source) ?: error("Fichier illisible")
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                when {
                    entry.isDirectory -> Unit
                    entry.name == BackupCodec.JSON_ENTRY -> json = zip.readBytes().decodeToString()
                    entry.name.startsWith(BackupCodec.PHOTOS_DIR) -> {
                        val name = entry.name.removePrefix(BackupCodec.PHOTOS_DIR)
                        // Les noms douteux (dossiers, « .. ») sont ignorés
                        if (BackupCodec.isSafePhotoName(name)) {
                            File(staging, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        val text = json ?: error("Ce fichier n'est pas une sauvegarde MyWhiskyBeer")
        return BackupCodec.decode(text)
    }

    private fun stagingDir(): File =
        File(context.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }

    /** Nom proposé pour le fichier : MyWhiskyBeer-2026-11-14.zip */
    fun suggestedFileName(): String {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
        return "MyWhiskyBeer-$date.zip"
    }

    private companion object {
        const val KEY_LAST_BACKUP = "last_backup_at"
    }
}
