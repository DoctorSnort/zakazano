package kz.chaykin.zakazano.ui.venueeditor

import androidx.lifecycle.ViewModel
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
import kz.chaykin.zakazano.data.repo.VenueRepository
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.model.Venue
import kz.chaykin.zakazano.ui.appContainer
import kz.chaykin.zakazano.ui.navigation.VenueEditorRoute
import androidx.lifecycle.createSavedStateHandle

data class VenueEditorState(
    val isNew: Boolean = true,
    val name: String = "",
    val address: String = "",
    val note: String = "",
    val rating: Rating? = null,
    val nameError: Boolean = false,
    val isSaved: Boolean = false,
    /** Заведение удалено — возвращаться нужно к списку, а не на его собственный экран. */
    val isDeleted: Boolean = false,
)

class VenueEditorViewModel(
    private val venueRepository: VenueRepository,
    private val venueId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(VenueEditorState(isNew = venueId == 0L))
    val state: StateFlow<VenueEditorState> = _state.asStateFlow()

    /** Исходное заведение нужно, чтобы при сохранении не потерять createdAt. */
    private var original: Venue? = null

    init {
        if (venueId != 0L) {
            viewModelScope.launch {
                val venue = venueRepository.observe(venueId).first() ?: return@launch
                original = venue
                _state.update {
                    it.copy(
                        isNew = false,
                        name = venue.name,
                        address = venue.address.orEmpty(),
                        note = venue.note.orEmpty(),
                        rating = venue.rating,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = false) }

    fun onAddressChange(value: String) = _state.update { it.copy(address = value) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }

    /** Повторное нажатие по выбранной оценке снимает её: заведение можно и не оценивать. */
    fun onRatingChange(value: Rating) =
        _state.update { it.copy(rating = if (it.rating == value) null else value) }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }

        viewModelScope.launch {
            venueRepository.save(
                Venue(
                    id = venueId,
                    name = current.name.trim(),
                    address = current.address.trim().ifBlank { null },
                    note = current.note.trim().ifBlank { null },
                    rating = current.rating,
                    createdAt = original?.createdAt ?: 0L,
                ),
            )
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (venueId == 0L) return
        viewModelScope.launch {
            venueRepository.delete(venueId)
            _state.update { it.copy(isSaved = true, isDeleted = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route: VenueEditorRoute = createSavedStateHandle().toRoute()
                VenueEditorViewModel(appContainer.venueRepository, route.venueId)
            }
        }
    }
}
