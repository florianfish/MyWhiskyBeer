package fr.florianfish.mywhiskybeer.ui.product

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.florianfish.mywhiskybeer.data.local.AppDatabase
import fr.florianfish.mywhiskybeer.data.local.ProductWriter
import fr.florianfish.mywhiskybeer.data.local.dao.ProductDao
import fr.florianfish.mywhiskybeer.data.local.entity.ProductWithDetails
import fr.florianfish.mywhiskybeer.data.photo.PhotoCleaner
import fr.florianfish.mywhiskybeer.data.photo.PhotoStore
import fr.florianfish.mywhiskybeer.domain.ProductForm
import fr.florianfish.mywhiskybeer.domain.toForm
import fr.florianfish.mywhiskybeer.ui.navigation.ProductEditRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProductEditState(
    val original: ProductWithDetails? = null,
    val form: ProductForm = ProductForm(),
    val photoPath: String? = null,
    val photoLoading: Boolean = false,
    val saving: Boolean = false,
) {
    val canSave: Boolean get() = original != null && !saving && form.name.isNotBlank()
}

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val productWriter: ProductWriter,
    private val photoStore: PhotoStore,
    private val photoCleaner: PhotoCleaner,
) : ViewModel() {

    private val productId = savedStateHandle.toRoute<ProductEditRoute>().productId

    private val _state = MutableStateFlow(ProductEditState())
    val state: StateFlow<ProductEditState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    private val importedPhotos = mutableSetOf<String>()
    private var committed = false

    init {
        viewModelScope.launch {
            productDao.observeById(productId).first()?.let { p ->
                _state.update { it.copy(original = p, form = p.toForm(), photoPath = p.product.photoPath) }
            }
        }
    }

    /** Le type n'est pas modifiable : les arômes des dégustations en dépendent. */
    fun updateForm(transform: (ProductForm) -> ProductForm) {
        _state.update { s -> s.copy(form = transform(s.form).copy(type = s.form.type)) }
    }

    fun newCaptureUri(): Uri = photoStore.newCaptureUri()

    fun onPhotoPicked(uri: Uri) = importPhoto { photoStore.import(uri) }

    fun onPhotoCaptured() = importPhoto { photoStore.importCapture() }

    fun removePhoto() {
        val old = _state.value.photoPath
        _state.update { it.copy(photoPath = null) }
        discardImported(old)
    }

    private fun importPhoto(block: suspend () -> String?) {
        viewModelScope.launch {
            _state.update { it.copy(photoLoading = true) }
            val path = block()
            if (path != null) {
                importedPhotos += path
                val old = _state.value.photoPath
                _state.update { it.copy(photoPath = path) }
                discardImported(old)
            }
            _state.update { it.copy(photoLoading = false) }
        }
    }

    /** Supprime tout de suite une photo importée pendant cette modification et finalement écartée. */
    private fun discardImported(path: String?) {
        if (path != null && path in importedPhotos) {
            importedPhotos -= path
            viewModelScope.launch { photoStore.delete(path) }
        }
    }

    fun save() {
        val s = _state.value
        val original = s.original ?: return
        if (!s.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            db.withTransaction { productWriter.update(original, s.form, s.photoPath) }
            committed = true
            // L'ancienne photo du produit est supprimée si plus rien ne l'utilise (souvent partagée avec une dégustation)
            original.product.photoPath?.takeIf { it != s.photoPath }?.let { photoCleaner.deleteIfUnused(it) }
            _saved.send(Unit)
        }
    }

    override fun onCleared() {
        if (!committed) importedPhotos.forEach { File(it).delete() }
    }
}
