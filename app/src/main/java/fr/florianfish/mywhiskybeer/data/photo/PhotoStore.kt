package fr.florianfish.mywhiskybeer.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Photos stockées dans le stockage privé de l'appli, redimensionnées et compressées
 * à l'import pour garder une base légère.
 */
@Singleton
class PhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val photosDir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    private val captureFile: File
        get() = File(File(context.cacheDir, "captures").apply { mkdirs() }, "capture.jpg")

    /** Uri temporaire à passer à l'appli caméra (ActivityResultContracts.TakePicture). */
    fun newCaptureUri(): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.photos", captureFile)

    /** Copie la photo dans l'appli (max 1600 px, JPEG 85 %, rotation EXIF appliquée). Renvoie son chemin. */
    suspend fun import(source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = max(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return@runCatching null

            var sample = 1
            while (longest / (sample * 2) >= MAX_SIZE) sample *= 2
            val decoded = resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val rotation = resolver.openInputStream(source)?.use { ExifInterface(it).rotationDegrees } ?: 0
            val scale = MAX_SIZE.toFloat() / max(decoded.width, decoded.height)
            val matrix = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

            val target = File(photosDir, "${UUID.randomUUID()}.jpg")
            target.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            if (result !== decoded) decoded.recycle()
            result.recycle()
            target.absolutePath
        }.getOrNull()
    }

    /** Importe la dernière photo prise avec l'appareil photo puis supprime le fichier temporaire. */
    suspend fun importCapture(): String? {
        val path = import(Uri.fromFile(captureFile))
        withContext(Dispatchers.IO) { captureFile.delete() }
        return path
    }

    suspend fun delete(path: String?) {
        if (path == null) return
        withContext(Dispatchers.IO) {
            File(path).takeIf { it.parentFile == photosDir }?.delete()
        }
    }

    private companion object {
        const val MAX_SIZE = 1600
        const val QUALITY = 85
    }
}
