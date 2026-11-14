package fr.florianfish.mywhiskybeer.ui.bottle

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.AppDatabase
import fr.florianfish.mywhiskybeer.data.local.ProductWriter
import fr.florianfish.mywhiskybeer.data.local.dao.CellarDao
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.entity.CellarItemEntity
import fr.florianfish.mywhiskybeer.data.local.entity.CellarStatus
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.barcode.BarcodeLookup
import fr.florianfish.mywhiskybeer.data.barcode.BarcodeResult
import fr.florianfish.mywhiskybeer.data.barcode.message
import fr.florianfish.mywhiskybeer.data.ocr.LabelScanner
import fr.florianfish.mywhiskybeer.data.photo.PhotoStore
import fr.florianfish.mywhiskybeer.domain.ProductForm
import fr.florianfish.mywhiskybeer.domain.blankToNull
import fr.florianfish.mywhiskybeer.domain.formatDecimal
import fr.florianfish.mywhiskybeer.domain.toDecimalOrNull
import fr.florianfish.mywhiskybeer.ui.navigation.BottleEditRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** Ce qui concerne la bouteille en stock (et non le produit lui-même). */
data class StockForm(
    val status: CellarStatus = CellarStatus.IN_STOCK,
    val quantity: Int = 1,
    /** Niveau restant en %, pour une bouteille entamée. */
    val fillLevel: Int = 75,
    val price: String = "",
    val shop: String = "",
    val purchaseDate: Long? = System.currentTimeMillis(),
)

data class BottleEditState(
    val existing: ProductWithDetails? = null,
    /** Le produit vient de la route (fiche produit) : on ne propose pas d'en changer. */
    val productLocked: Boolean = false,
    /** La bouteille était déjà en cave : on modifie au lieu d'ajouter. */
    val editing: Boolean = false,
    val product: ProductForm = ProductForm(),
    val stock: StockForm = StockForm(),
    /** Photo du nouveau produit (scan de l'étiquette ou photo). */
    val photoPath: String? = null,
    val photoLoading: Boolean = false,
    val scanning: Boolean = false,
    val scannedLines: List<String> = emptyList(),
    /** Recherche du code-barres en cours. */
    val barcodeBusy: Boolean = false,
    /** Résultat du dernier scan de code-barres. */
    val barcodeMessage: String? = null,
    val saving: Boolean = false,
) {
    val type: ProductType get() = existing?.product?.type ?: product.type
    val canSave: Boolean get() = !saving && (existing != null || product.name.isNotBlank())
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BottleEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val cellarDao: CellarDao,
    private val productWriter: ProductWriter,
    private val photoStore: PhotoStore,
    private val labelScanner: LabelScanner,
    private val barcodeLookup: BarcodeLookup,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<BottleEditRoute>()

    private val _state = MutableStateFlow(
        BottleEditState(
            productLocked = route.productId != null,
            stock = if (route.wishlist) StockForm(status = CellarStatus.WISHLIST, purchaseDate = null) else StockForm(),
        ),
    )
    val state: StateFlow<BottleEditState> = _state.asStateFlow()

    val suggestions: StateFlow<List<ProductWithDetails>> = _state
        .map { if (it.existing == null) it.product.name.trim() else "" }
        .distinctUntilChanged()
        .debounce(250)
        .flatMapLatest { query -> if (query.length < 2) flowOf(emptyList()) else productDao.search(query) }
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    private val importedPhotos = mutableSetOf<String>()
    private var committed = false

    init {
        route.productId?.let { id ->
            viewModelScope.launch {
                productDao.observeById(id).first()?.let { selectExisting(it) }
            }
        }
    }

    fun updateProduct(transform: (ProductForm) -> ProductForm) {
        _state.update { it.copy(product = transform(it.product)) }
    }

    fun updateStock(transform: (StockForm) -> StockForm) {
        _state.update { it.copy(stock = transform(it.stock)) }
    }

    fun setStatus(status: CellarStatus) {
        updateStock { s ->
            s.copy(
                status = status,
                // Une bouteille achetée a une date d'achat ; une envie d'achat n'en a pas
                purchaseDate = when {
                    status == CellarStatus.WISHLIST -> null
                    s.purchaseDate == null -> System.currentTimeMillis()
                    else -> s.purchaseDate
                },
            )
        }
    }

    /** Produit déjà en base : si la bouteille est déjà en cave, on reprend ses valeurs. */
    fun selectExisting(product: ProductWithDetails) {
        viewModelScope.launch {
            val item = cellarDao.getForProduct(product.product.id)
            _state.update { s ->
                s.copy(
                    existing = product,
                    scannedLines = emptyList(),
                    editing = item != null,
                    stock = item?.toForm() ?: s.stock,
                )
            }
        }
    }

    fun clearExisting() {
        _state.update { s ->
            val p = s.existing?.product
            s.copy(
                existing = null,
                editing = false,
                product = if (p != null) s.product.copy(type = p.type, name = p.name) else s.product,
            )
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

    fun onLabelCaptured() {
        viewModelScope.launch {
            _state.update { it.copy(scanning = true) }
            val scan = labelScanner.scanCapture()
            if (scan != null) {
                setPhoto(scan.photoPath)
                _state.update {
                    it.copy(product = it.product.withGuess(scan.guess), scannedLines = scan.lines.map { l -> l.text })
                }
            }
            _state.update { it.copy(scanning = false) }
        }
    }

    fun removePhoto() {
        val old = _state.value.photoPath
        _state.update { it.copy(photoPath = null) }
        discard(old)
    }

    private fun importPhoto(block: suspend () -> String?) {
        viewModelScope.launch {
            _state.update { it.copy(photoLoading = true) }
            block()?.let(::setPhoto)
            _state.update { it.copy(photoLoading = false) }
        }
    }

    private fun setPhoto(path: String) {
        importedPhotos += path
        val old = _state.value.photoPath
        _state.update { it.copy(photoPath = path) }
        discard(old)
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
            db.withTransaction {
                val existing = s.existing?.product
                val id = if (existing != null) {
                    productWriter.setPhotoIfMissing(existing, s.photoPath)
                    productWriter.setBarcodeIfMissing(existing, s.product.barcode)
                    existing.id
                } else {
                    productWriter.insert(s.product, s.photoPath)
                }
                cellarDao.upsert(s.stock.toEntity(id))
            }
            committed = true
            _saved.send(Unit)
        }
    }

    override fun onCleared() {
        if (!committed) importedPhotos.forEach { File(it).delete() }
    }
}

private fun CellarItemEntity.toForm() = StockForm(
    status = status,
    quantity = quantity.coerceAtLeast(1),
    fillLevel = fillLevel?.takeIf { status == CellarStatus.OPENED } ?: 75,
    price = purchasePrice?.let(::formatDecimal).orEmpty(),
    shop = shop.orEmpty(),
    purchaseDate = purchaseDate,
)

private fun StockForm.toEntity(productId: Long) = CellarItemEntity(
    productId = productId,
    status = status,
    quantity = quantity.coerceAtLeast(1),
    fillLevel = when (status) {
        CellarStatus.IN_STOCK -> 100
        CellarStatus.OPENED -> fillLevel.coerceIn(1, 99)
        CellarStatus.EMPTY -> 0
        CellarStatus.WISHLIST -> null
    },
    purchaseDate = if (status == CellarStatus.WISHLIST) null else purchaseDate,
    purchasePrice = price.toDecimalOrNull(),
    shop = shop.blankToNull(),
)
