package kz.chaykin.zakazano.ui.venuedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.Currency
import kz.chaykin.zakazano.model.Item
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.ItemSort
import kz.chaykin.zakazano.ui.components.CatIcons
import kz.chaykin.zakazano.ui.components.EmptyState
import kz.chaykin.zakazano.ui.components.ItemRow
import kz.chaykin.zakazano.ui.components.RatingBadge
import kz.chaykin.zakazano.ui.components.RatingFilterRow

private const val TAB_COUNT = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDetailScreen(
    onBack: () -> Unit,
    onEditVenue: (Long) -> Unit,
    onAddItem: (venueId: Long, kind: ItemKind) -> Unit,
    onOpenItem: (venueId: Long, itemId: Long) -> Unit,
    viewModel: VenueDetailViewModel = viewModel(factory = VenueDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val scope = rememberCoroutineScope()
    val currentKind = if (pagerState.currentPage == 0) ItemKind.DISH else ItemKind.DRINK

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.venue?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    ItemSortMenu(current = state.sort, onSelect = viewModel::onSortChange)
                    IconButton(onClick = { onEditVenue(viewModel.venueId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.venue_edit))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddItem(viewModel.venueId, currentKind) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        stringResource(
                            if (currentKind == ItemKind.DISH) R.string.item_add_dish else R.string.item_add_drink,
                        ),
                    )
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VenueHeader(state = state)

            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                listOf(R.string.tab_dishes, R.string.tab_drinks).forEachIndexed { index, labelRes ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(labelRes)) },
                    )
                }
            }

            RatingFilterRow(
                selected = state.ratingFilter,
                onToggle = viewModel::toggleRatingFilter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )

            HorizontalDivider()

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val isDishes = page == 0
                ItemList(
                    items = if (isDishes) state.dishes else state.drinks,
                    hasAny = if (isDishes) state.hasDishes else state.hasDrinks,
                    currency = state.currency,
                    emptyTitleRes = if (isDishes) R.string.items_empty_dishes else R.string.items_empty_drinks,
                    onOpenItem = { itemId -> onOpenItem(viewModel.venueId, itemId) },
                )
            }
        }
    }
}

@Composable
private fun VenueHeader(state: VenueDetailState) {
    val venue = state.venue ?: return
    val hasSubtitle = !venue.address.isNullOrBlank() || !venue.note.isNullOrBlank() || venue.rating != null
    if (!hasSubtitle) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            venue.rating?.let { RatingBadge(it) }
            venue.address?.takeIf { it.isNotBlank() }?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        venue.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ItemList(
    items: List<Item>,
    hasAny: Boolean,
    currency: Currency,
    emptyTitleRes: Int,
    onOpenItem: (Long) -> Unit,
) {
    when {
        !hasAny -> EmptyState(
            icon = CatIcons.EmptyBowl,
            title = stringResource(emptyTitleRes),
            text = stringResource(R.string.items_empty_text),
        )

        items.isEmpty() -> EmptyState(
            icon = CatIcons.Sleeping,
            title = stringResource(R.string.items_nothing_matches),
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id }) { item ->
                ItemRow(item = item, currency = currency, onClick = { onOpenItem(item.id) })
            }
        }
    }
}

@Composable
private fun ItemSortMenu(current: ItemSort, onSelect: (ItemSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        ItemSort.NAME to R.string.sort_name,
        ItemSort.RATING to R.string.sort_rating,
        ItemSort.PRICE to R.string.sort_price,
    )

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (sort, labelRes) ->
            DropdownMenuItem(
                text = { Text(stringResource(labelRes)) },
                leadingIcon = { RadioButton(selected = sort == current, onClick = null) },
                onClick = {
                    onSelect(sort)
                    expanded = false
                },
            )
        }
    }
}
