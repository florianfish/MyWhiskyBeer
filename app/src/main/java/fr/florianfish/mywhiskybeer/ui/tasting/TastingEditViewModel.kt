package fr.florianfish.mywhiskybeer.ui.tasting

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.AppDatabase
import fr.florianfish.mywhiskybeer.data.local.ProductWriter
import fr.florianfish.mywhiskybeer.data.local.dao.AromaDao
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProducerInfo
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.barcode.BarcodeLookup
import fr.florianfish.mywhiskybeer.data.barcode.BarcodeResult
import fr.florianfish.mywhiskybeer.data.barcode.message
import fr.florianfish.mywhiskybeer.data.ocr.LabelScanner
import fr.florianfish.mywhiskybeer.data.photo.PhotoCleaner
import fr.florianfish.mywhiskybeer.data.photo.PhotoStore
import fr.florianfish.mywhiskybeer.domain.ProducerSuggestions
import fr.florianfish.mywhiskybeer.domain.ProductForm
import fr.florianfish.mywhiskybeer.domain.blankToNull
import fr.florianfish.mywhiskybeer.ui.navigation.TastingEditRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TastingForm(
    val rating: Int = 70,
    val nose: String = "",
    val palate: String = "",
    val finish: String = "",
    val comment: String = "",
    val place: String = "",
    val servedAs: String = "",
    /** aromaId → intensité (1..3). */
    val aromas: Map<Long, Int> = emptyMap(),
    val photoPath: String? = null,
)

data class TastingEditState(
    /** Produit déjà en base (dégustation d'un produit connu), sinon null. */
    val existing: ProductWithDetails? = null,
    val product: ProductForm = ProductForm(),
    val tasting: TastingForm = TastingForm(),
    val saving: Boolean = false,
    val photoLoading: Boolean = false,
    val scanning: Boolean = false,
    /** Lignes lues sur l'étiquette, proposées pour remplir le nom ou le producteur. */
    val scannedLines: List<String> = emptyList(),
    /** Recherche du code-barres en cours. */
    val barcodeBusy: Boolean = false,
    /** Résultat du dernier scan de code-barres. */
    val barcodeMessage: String? = null,
    /** Modification d'une dégustation existante (sinon nouvelle dégustation). */
    val editing: Boolean = false,
) {
    val type: ProductType get() = existing?.product?.type ?: product.type
    val canSave: Boolean get() = !saving && (existing != null || product.name.isNotBlank())
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TastingEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val tastingDao: TastingDao,
    aromaDao: AromaDao,
    private val productWriter: ProductWriter,
    private val photoStore: PhotoStore,
    private val labelScanner: LabelScanner,
    private val barcodeLookup: BarcodeLookup,
    private val photoCleaner: PhotoCleaner,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<TastingEditRoute>()

    private val _state = MutableStateFlow(TastingEditState())
    val state: StateFlow<TastingEditState> = _state.asStateFlow()

    val aromas: StateFlow<List<AromaEntity>> = _state
        .map { it.type }
        .distinctUntilChanged()
        .flatMapLatest { aromaDao.observeByType(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Produits existants qui correspondent au nom saisi, pour éviter les doublons. */
    val suggestions: StateFlow<List<ProductWithDetails>> = _state
        .map { if (it.existing == null) it.product.name.trim() else "" }
        .distinctUntilChanged()
        .debounce(250)
        .flatMapLatest { query -> if (query.length < 2) flowOf(emptyList()) else productDao.search(query) }
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Distilleries / brasseries déjà connues qui correspondent au producteur saisi. */
    val producerSuggestions: StateFlow<List<ProducerInfo>> = combine(
        productDao.observeProducerInfos(),
        _state.map { it.product.type to it.product.producer }.distinctUntilChanged(),
    ) { known, (type, typed) -> ProducerSuggestions.match(known, type, typed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pickProducer(info: ProducerInfo) = updateProduct { it.withProducer(info) }

    private val _saved = Channel<Long>(Channel.BUFFERED)
    /** Émet l'id du produit une fois la dégustation enregistrée. */
    val saved = _saved.receiveAsFlow()

    /** Photos importées pendant cette saisie, supprimées si elles ne sont pas gardées. */
    private val importedPhotos = mutableSetOf<String>()
    private var committed = false

    /** Dégustation en cours de modification, telle qu'en base. */
    private var original: TastingEntity? = null

    init {
        val tastingId = route.tastingId
        if (tastingId != null) {
            viewModelScope.launch { loadForEdit(tastingId) }
        } else {
            viewModelScope.launch {
                val lastPlace = tastingDao.lastPlace().orEmpty()
                _state.update { it.copy(tasting = it.tasting.copy(place = lastPlace)) }
            }
            route.productId?.let { id ->
                viewModelScope.launch {
                    productDao.observeById(id).first()?.let { selectExisting(it) }
                }
            }
        }
    }

    private suspend fun loadForEdit(tastingId: Long) {
        val t = tastingDao.getById(tastingId) ?: return
        val product = productDao.observeById(t.productId).first() ?: return
        val aromas = tastingDao.aromaRefs(tastingId).associate { it.aromaId to it.intensity }
        original = t
        _state.update {
            it.copy(
                editing = true,
                existing = product,
                tasting = TastingForm(
                    rating = t.rating,
                    nose = t.nose.orEmpty(),
                    palate = t.palate.orEmpty(),
                    finish = t.finish.orEmpty(),
                    comment = t.comment.orEmpty(),
                    place = t.place.orEmpty(),
                    servedAs = t.servedAs.orEmpty(),
                    aromas = aromas,
                    photoPath = t.photoPath,
                ),
            )
        }
    }

    fun updateProduct(transform: (ProductForm) -> ProductForm) {
        _state.update { s ->
            val updated = transform(s.product)
            // Changer de type invalide les arômes cochés (référentiels différents)
            val tasting = if (updated.type != s.product.type) s.tasting.copy(aromas = emptyMap()) else s.tasting
            s.copy(product = updated, tasting = tasting)
        }
    }

    fun updateTasting(transform: (TastingForm) -> TastingForm) {
        _state.update { it.copy(tasting = transform(it.tasting)) }
    }

    fun selectExisting(product: ProductWithDetails) {
        _state.update { s ->
            val typeChanged = s.type != product.product.type
            s.copy(
                existing = product,
                scannedLines = emptyList(),
                tasting = if (typeChanged) s.tasting.copy(aromas = emptyMap()) else s.tasting,
            )
        }
    }

    fun clearExisting() {
        _state.update { s ->
            val p = s.existing?.product
            s.copy(
                existing = null,
                product = if (p != null) s.product.copy(type = p.type, name = p.name) else s.product,
            )
        }
    }

    /** Fait défiler l'intensité d'un arôme : aucun → léger → marqué → dominant → aucun. */
    fun cycleAroma(aromaId: Long) {
        updateTasting { t ->
            val next = ((t.aromas[aromaId] ?: 0) + 1) % 4
            t.copy(aromas = if (next == 0) t.aromas - aromaId else t.aromas + (aromaId to next))
        }
    }

    /**
     * Code-barres scanné : produit du carnet s'il est connu (hors-ligne), sinon Open Food Facts
     * pour pré-remplir les champs vides. Le code est gardé avec le produit.
     */
    fun onBarcodeScanned(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(barcodeBusy = true, barcodeMessage = null) }
            val result = barcodeLookup.lookup(code)
            when (result) {
                is BarcodeResult.Existing -> selectExisting(result.product)
                is BarcodeResult.Found -> updateProduct { it.withGuess(result.guess).copy(barcode = code) }
                BarcodeResult.Invalid -> Unit
                else -> updateProduct { it.copy(barcode = code) }
            }
            _state.update { it.copy(barcodeBusy = false, barcodeMessage = result.message(code)) }
        }
    }

    fun newCaptureUri(): Uri = photoStore.newCaptureUri()

    fun onPhotoPicked(uri: Uri) = importPhoto { photoStore.import(uri) }

    fun onPhotoCaptured() = importPhoto { photoStore.importCapture() }

    /** Photo de l'étiquette : devient la photo de la dégustation et pré-remplit le produit. */
    fun onLabelCaptured() {
        viewModelScope.launch {
            _state.update { it.copy(scanning = true) }
            val scan = labelScanner.scanCapture()
            if (scan != null) {
                importedPhotos += scan.photoPath
                val old = _state.value.tasting.photoPath
                updateProduct { it.withGuess(scan.guess) }
                updateTasting { it.copy(photoPath = scan.photoPath) }
                _state.update { it.copy(scannedLines = scan.lines.map { l -> l.text }) }
                discard(old)
            }
            _state.update { it.copy(scanning = false) }
        }
    }

    fun removePhoto() {
        val old = _state.value.tasting.photoPath
        updateTasting { it.copy(photoPath = null) }
        discard(old)
    }

    private fun importPhoto(block: suspend () -> String?) {
        viewModelScope.launch {
            _state.update { it.copy(photoLoading = true) }
            val path = block()
            val old = _state.value.tasting.photoPath
            if (path != null) {
                importedPhotos += path
                updateTasting { it.copy(photoPath = path) }
                discard(old)
            }
            _state.update { it.copy(photoLoading = false) }
        }
    }

    private fun discard(path: String?) {
        if (path != null && path in importedPhotos) {
            importedPhotos -= path
            viewModelScope.launch { photoStore.delete(path) }
        }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val t = s.tasting
            val productId = db.withTransaction {
                val existing = s.existing?.product
                val id = if (existing != null) {
                    // Le produit prend la photo de sa première dégustation
                    productWriter.setPhotoIfMissing(existing, t.photoPath)
                    productWriter.setBarcodeIfMissing(existing, s.product.barcode)
                    existing.id
                } else {
                    productWriter.insert(s.product, t.photoPath)
                }
                tastingDao.save(
                    TastingEntity(
                        // En modification : même identifiant et même date qu'avant
                        id = original?.id ?: 0,
                        date = original?.date ?: System.currentTimeMillis(),
                        productId = id,
                        rating = t.rating,
                        nose = t.nose.blankToNull(),
                        palate = t.palate.blankToNull(),
                        finish = t.finish.blankToNull(),
                        comment = t.comment.blankToNull(),
                        place = t.place.blankToNull(),
                        servedAs = t.servedAs.blankToNull(),
                        photoPath = t.photoPath,
                    ),
                    t.aromas,
                )
                id
            }
            committed = true
            // Photo remplacée ou retirée : l'ancien fichier est supprimé s'il ne sert plus
            original?.photoPath?.takeIf { it != t.photoPath }?.let { photoCleaner.deleteIfUnused(it) }
            _saved.send(productId)
        }
    }

    override fun onCleared() {
        // Saisie abandonnée : on ne laisse pas traîner les photos importées
        if (!committed) importedPhotos.forEach { path -> File(path).delete() }
    }
}
