package fr.florianfish.mywhiskybeer.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.florianfish.mywhiskybeer.data.photo.PhotoStore
import fr.florianfish.mywhiskybeer.domain.LabelGuess
import fr.florianfish.mywhiskybeer.domain.LabelParser
import fr.florianfish.mywhiskybeer.domain.OcrLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Lit le texte d'une photo d'étiquette avec ML Kit (modèle embarqué : fonctionne hors-ligne). */
@Singleton
class LabelReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun read(path: String): List<OcrLine> = withContext(Dispatchers.IO) {
        runCatching {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(path)))
            val text = suspendCancellableCoroutine<Text> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            text.textBlocks.flatMap { it.lines }.map { OcrLine(it.text, it.boundingBox?.height() ?: 0) }
        }.getOrDefault(emptyList())
    }
}

/** Résultat d'un scan : la photo (gardée comme photo du produit), le texte lu et ce qu'on en déduit. */
data class LabelScan(val photoPath: String, val lines: List<OcrLine>, val guess: LabelGuess)

@Singleton
class LabelScanner @Inject constructor(
    private val photoStore: PhotoStore,
    private val reader: LabelReader,
) {
    /** Importe la photo prise avec l'appareil photo, puis l'analyse. */
    suspend fun scanCapture(): LabelScan? {
        val path = photoStore.importCapture() ?: return null
        val lines = reader.read(path)
        return LabelScan(path, lines, LabelParser.parse(lines))
    }
}
