package kz.chaykin.zakazano.ui.venuelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.data.repo.VenueRepository
import kz.chaykin.zakazano.model.VenueSort
import kz.chaykin.zakazano.model.VenueSummary
import kz.chaykin.zakazano.ui.appContainer
import kz.chaykin.zakazano.util.filterByQuery
import kz.chaykin.zakazano.util.orderBy

data class VenueListState(
    val venues: List<VenueSummary> = emptyList(),
    val query: String = "",
    val sort: VenueSort = VenueSort.NAME,
    val isLoaded: Boolean = false,
    /** Список пуст, потому что ничего ещё не добавлено, а не потому что не нашлось по запросу. */
    val isLibraryEmpty: Boolean = false,
)

class VenueListViewModel(
    private val venueRepository: VenueRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(VenueSort.NAME)

    val state: StateFlow<VenueListState> =
        combine(venueRepository.observeSummaries(), query, sort) { all, query, sort ->
            VenueListState(
                venues = all.filterByQuery(query).orderBy(sort),
                query = query,
                sort = sort,
                isLoaded = true,
                isLibraryEmpty = all.isEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = VenueListState(),
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onSortChange(value: VenueSort) {
        sort.value = value
    }

    fun delete(venueId: Long) {
        viewModelScope.launch { venueRepository.delete(venueId) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { VenueListViewModel(appContainer.venueRepository) }
        }
    }
}
