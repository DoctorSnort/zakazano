package kz.chaykin.zakazano.ui.venuedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kz.chaykin.zakazano.data.prefs.SettingsStore
import kz.chaykin.zakazano.data.repo.ItemRepository
import kz.chaykin.zakazano.data.repo.VenueRepository
import kz.chaykin.zakazano.model.Currency
import kz.chaykin.zakazano.model.Item
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.ItemSort
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.model.Venue
import kz.chaykin.zakazano.ui.appContainer
import kz.chaykin.zakazano.ui.navigation.VenueDetailRoute
import kz.chaykin.zakazano.util.filterByRatings
import kz.chaykin.zakazano.util.orderBy

/** Как показывать списки внутри вкладок. Один объект, чтобы не плодить combine на шесть потоков. */
private data class ViewOptions(
    val ratingFilter: Set<Rating> = emptySet(),
    val sort: ItemSort = ItemSort.NAME,
)

data class VenueDetailState(
    val venue: Venue? = null,
    val dishes: List<Item> = emptyList(),
    val drinks: List<Item> = emptyList(),
    /** Есть ли записи вообще — чтобы отличить «ничего не добавлено» от «фильтр всё скрыл». */
    val hasDishes: Boolean = false,
    val hasDrinks: Boolean = false,
    val ratingFilter: Set<Rating> = emptySet(),
    val sort: ItemSort = ItemSort.NAME,
    val currency: Currency = Currency.Default,
)

class VenueDetailViewModel(
    venueRepository: VenueRepository,
    itemRepository: ItemRepository,
    settingsStore: SettingsStore,
    val venueId: Long,
) : ViewModel() {

    private val options = MutableStateFlow(ViewOptions())

    private val dishes: Flow<List<Item>> = itemRepository.observeByVenue(venueId, ItemKind.DISH)
    private val drinks: Flow<List<Item>> = itemRepository.observeByVenue(venueId, ItemKind.DRINK)

    val state: StateFlow<VenueDetailState> = combine(
        venueRepository.observe(venueId),
        dishes,
        drinks,
        options,
        settingsStore.settings,
    ) { venue, allDishes, allDrinks, options, settings ->
        VenueDetailState(
            venue = venue,
            dishes = allDishes.filterByRatings(options.ratingFilter).orderBy(options.sort),
            drinks = allDrinks.filterByRatings(options.ratingFilter).orderBy(options.sort),
            hasDishes = allDishes.isNotEmpty(),
            hasDrinks = allDrinks.isNotEmpty(),
            ratingFilter = options.ratingFilter,
            sort = options.sort,
            currency = settings.currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = VenueDetailState(),
    )

    fun toggleRatingFilter(rating: Rating) = options.update { current ->
        val next = if (rating in current.ratingFilter) {
            current.ratingFilter - rating
        } else {
            current.ratingFilter + rating
        }
        current.copy(ratingFilter = next)
    }

    fun onSortChange(sort: ItemSort) = options.update { it.copy(sort = sort) }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val route: VenueDetailRoute = createSavedStateHandle().toRoute()
                VenueDetailViewModel(
                    venueRepository = appContainer.venueRepository,
                    itemRepository = appContainer.itemRepository,
                    settingsStore = appContainer.settingsStore,
                    venueId = route.venueId,
                )
            }
        }
    }
}
