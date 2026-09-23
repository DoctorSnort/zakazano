package kz.chaykin.zakazano.ui.venuelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.data.repo.BiomeRepository
import kz.chaykin.zakazano.data.repo.VenueRepository
import kz.chaykin.zakazano.model.Biome
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
    val biomes: List<Biome> = emptyList(),
    val currentBiome: Biome? = null,
) {
    /** Пока биом один, главный экран выглядит как до биомов — с названием приложения. */
    val hasSeveralBiomes: Boolean get() = biomes.size > 1
}

private data class Filters(val query: String, val sort: VenueSort)

class VenueListViewModel(
    private val venueRepository: VenueRepository,
    private val biomeRepository: BiomeRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(VenueSort.NAME)
    private val filters = combine(query, sort, ::Filters)

    /**
     * Биом и его заведения приходят парой. Если собирать их из двух независимых потоков,
     * при переключении на кадр видно новое название над старым списком.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentWithVenues = biomeRepository.current.flatMapLatest { biome ->
        venueRepository.observeSummaries(biome.id).map { venues -> biome to venues }
    }

    val state: StateFlow<VenueListState> =
        combine(
            currentWithVenues,
            filters,
            biomeRepository.biomes,
        ) { (current, all), filters, biomes ->
            VenueListState(
                venues = all.filterByQuery(filters.query).orderBy(filters.sort),
                query = filters.query,
                sort = filters.sort,
                isLoaded = true,
                isLibraryEmpty = all.isEmpty(),
                biomes = biomes,
                currentBiome = current,
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

    fun moveVenue(venueId: Long, biomeId: Long) {
        viewModelScope.launch { venueRepository.moveToBiome(venueId, biomeId) }
    }

    fun selectBiome(id: Long) {
        // Поиск из прошлого биома в новом только сбивает с толку.
        query.value = ""
        viewModelScope.launch { biomeRepository.select(id) }
    }

    fun createBiome(name: String) {
        if (name.isBlank()) return
        query.value = ""
        viewModelScope.launch { biomeRepository.create(name) }
    }

    fun renameBiome(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { biomeRepository.rename(id, name) }
    }

    fun deleteBiome(id: Long) {
        viewModelScope.launch { biomeRepository.delete(id) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                VenueListViewModel(appContainer.venueRepository, appContainer.biomeRepository)
            }
        }
    }
}
