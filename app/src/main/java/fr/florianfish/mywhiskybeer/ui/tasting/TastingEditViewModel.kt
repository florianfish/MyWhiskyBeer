package fr.florianfish.mywhiskybeer.ui.tasting

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.AppDatabase
import fr.florianfish.mywhiskybeer.data.local.dao.AromaDao
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.dao.TastingDao
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerDetailsEntity
import fr.florianfish.mywhiskybeer.data.local.entity.PeatLevel
import fr.florianfish.mywhiskybeer.data.local.entity.ProductEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.local.entity.TastingEntity
import fr.florianfish.mywhiskybeer.data.local.entity.WhiskyDetailsEntity
import fr.florianfish.mywhiskybeer.data.photo.PhotoStore
import fr.florianfish.mywhiskybeer.ui.navigation.TastingEditRoute
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
import javax.inject.Inject

/** Champs du produit, saisis sous forme de texte (convertis à l'enregistrement). */
data class ProductForm(
    val type: ProductType = ProductType.WHISKY,
    val name: String = "",
    val producer: String = "",
    val country: String = "",
    val region: String = "",
    val abv: String = "",
    // Bière
    val style: String = "",
    val ibu: String = "",
    val color: String = "",
    // Whisky
    val category: String = "",
    val age: String = "",
    val caskType: String = "",
    val peat: PeatLevel = PeatLevel.NONE,
    val caskStrength: Boolean = false,
)

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
    private val photoStore: PhotoStore,
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

    val producers: StateFlow<List<String>> = _state
        .map { it.type }
        .distinctUntilChanged()
        .flatMapLatest { productDao.observeProducers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saved = Channel<Long>(Channel.BUFFERED)
    /** Émet l'id du produit une fois la dégustation enregistrée. */
    val saved = _saved.receiveAsFlow()

    /** Photos importées pendant cette saisie, supprimées si elles ne sont pas gardées. */
    private val importedPhotos = mutableSetOf<String>()
    private var committed = false

    init {
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

    fun newCaptureUri(): Uri = photoStore.newCaptureUri()

    fun onPhotoPicked(uri: Uri) = importPhoto { photoStore.import(uri) }

    fun onPhotoCaptured() = importPhoto { photoStore.importCapture() }

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
                val id = s.existing?.product?.id ?: insertProduct(s.product, t.photoPath)
                // Le produit prend la photo de sa première dégustation
                val existingProduct = s.existing?.product
                if (existingProduct != null && existingProduct.photoPath == null && t.photoPath != null) {
                    productDao.updateProduct(existingProduct.copy(photoPath = t.photoPath))
                }
                tastingDao.save(
                    TastingEntity(
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
            _saved.send(productId)
        }
    }

    private suspend fun insertProduct(form: ProductForm, photoPath: String?): Long {
        val product = ProductEntity(
            type = form.type,
            name = form.name.trim(),
            producer = form.producer.blankToNull(),
            country = form.country.blankToNull(),
            region = form.region.blankToNull(),
            abv = form.abv.toDecimalOrNull(),
            photoPath = photoPath,
        )
        return when (form.type) {
            ProductType.BEER -> productDao.insertBeer(
                product,
                BeerDetailsEntity(
                    productId = 0,
                    style = form.style.blankToNull(),
                    ibu = form.ibu.trim().toIntOrNull(),
                    color = form.color.blankToNull(),
                ),
            )
            ProductType.WHISKY -> productDao.insertWhisky(
                product,
                WhiskyDetailsEntity(
                    productId = 0,
                    category = form.category.blankToNull(),
                    age = form.age.trim().toIntOrNull(),
                    caskType = form.caskType.blankToNull(),
                    peat = form.peat,
                    caskStrength = form.caskStrength,
                ),
            )
        }
    }

    override fun onCleared() {
        // Saisie abandonnée : on ne laisse pas traîner les photos importées
        if (!committed) importedPhotos.forEach { path -> java.io.File(path).delete() }
    }
}

private fun String.blankToNull(): String? = trim().ifEmpty { null }

/** Accepte la virgule française : « 46,3 » → 46.3. */
internal fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()
