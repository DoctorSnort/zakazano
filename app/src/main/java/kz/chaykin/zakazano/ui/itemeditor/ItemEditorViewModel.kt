package kz.chaykin.zakazano.ui.itemeditor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.data.prefs.SettingsStore
import kz.chaykin.zakazano.data.repo.ItemRepository
import kz.chaykin.zakazano.model.Currency
import kz.chaykin.zakazano.model.Item
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Photo
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.ui.appContainer
import kz.chaykin.zakazano.ui.navigation.ItemEditorRoute
import kz.chaykin.zakazano.util.PriceFormat
import java.io.File

data class ItemEditorState(
    val isNew: Boolean = true,
    val name: String = "",
    val kind: ItemKind = ItemKind.DISH,
    val rating: Rating = Rating.GOOD,
    val price: String = "",
    val comment: String = "",
    val photos: List<Photo> = emptyList(),
    val currency: Currency = Currency.Default,
    val nameError: Boolean = false,
    val priceError: Boolean = false,
    val isSaved: Boolean = false,
)

class ItemEditorViewModel(
    private val itemRepository: ItemRepository,
    settingsStore: SettingsStore,
    private val venueId: Long,
    private val itemId: Long,
    initialKind: ItemKind,
) : ViewModel() {

    private val _state = MutableStateFlow(ItemEditorState(isNew = itemId == 0L, kind = initialKind))
    val state: StateFlow<ItemEditorState> = _state.asStateFlow()

    private var original: Item? = null

    init {
        viewModelScope.launch {
            val currency = settingsStore.settings.first().currency
            _state.update { it.copy(currency = currency) }
        }

        if (itemId != 0L) {
            viewModelScope.launch {
                val item = itemRepository.observe(itemId).first() ?: return@launch
                original = item
                _state.update {
                    it.copy(
                        isNew = false,
                        name = item.name,
                        kind = item.kind,
                        rating = item.rating,
                        price = item.priceMinor?.let(PriceFormat::toInput).orEmpty(),
                        comment = item.comment.orEmpty(),
                        photos = item.photos,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = false) }

    fun onKindChange(value: ItemKind) = _state.update { it.copy(kind = value) }

    fun onRatingChange(value: Rating) = _state.update { it.copy(rating = value) }

    fun onPriceChange(value: String) = _state.update { it.copy(price = value, priceError = false) }

    fun onCommentChange(value: String) = _state.update { it.copy(comment = value) }

    /** Файл под снимок системной камеры: её надо запускать уже зная, куда писать. */
    fun newCameraTarget(): File = itemRepository.newCameraTarget()

    fun addPhotoFromGallery(uri: Uri) = viewModelScope.launch {
        val photo = itemRepository.importPhoto(uri)
        _state.update { it.copy(photos = it.photos + photo) }
    }

    fun addPhotoFromCamera(file: File) = viewModelScope.launch {
        val photo = itemRepository.importPhoto(file)
        _state.update { it.copy(photos = it.photos + photo) }
    }

    fun removePhoto(photo: Photo) = _state.update { it.copy(photos = it.photos - photo) }

    fun save() {
        val current = _state.value

        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }

        val priceMinor = if (current.price.isBlank()) {
            null
        } else {
            PriceFormat.parse(current.price) ?: run {
                _state.update { it.copy(priceError = true) }
                return
            }
        }

        viewModelScope.launch {
            itemRepository.save(
                Item(
                    id = itemId,
                    venueId = venueId,
                    name = current.name.trim(),
                    kind = current.kind,
                    rating = current.rating,
                    priceMinor = priceMinor,
                    comment = current.comment.trim().ifBlank { null },
                    photos = current.photos,
                    createdAt = original?.createdAt ?: 0L,
                ),
            )
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (itemId == 0L) return
        viewModelScope.launch {
            itemRepository.delete(itemId)
            _state.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route: ItemEditorRoute = createSavedStateHandle().toRoute()
                ItemEditorViewModel(
                    itemRepository = appContainer.itemRepository,
                    settingsStore = appContainer.settingsStore,
                    venueId = route.venueId,
                    itemId = route.itemId,
                    initialKind = route.kind,
                )
            }
        }
    }
}
