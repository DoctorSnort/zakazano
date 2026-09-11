package kz.chaykin.zakazano.ui.venuelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.VenueSort
import kz.chaykin.zakazano.model.VenueSummary
import kz.chaykin.zakazano.ui.components.CatIcons
import kz.chaykin.zakazano.ui.components.EmptyState
import kz.chaykin.zakazano.ui.components.RatingSummaryBadge
import kz.chaykin.zakazano.util.effectiveRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueListScreen(
    onOpenVenue: (Long) -> Unit,
    onAddVenue: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: VenueListViewModel = viewModel(factory = VenueListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchVisible) {
        if (searchVisible) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchVisible) {
                        SearchField(
                            query = state.query,
                            onQueryChange = viewModel::onQueryChange,
                            focusRequester = focusRequester,
                        )
                    } else {
                        Text(stringResource(R.string.venues_title))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) viewModel.onQueryChange("")
                        },
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(R.string.venues_search),
                        )
                    }
                    SortMenu(current = state.sort, onSelect = viewModel::onSortChange)
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddVenue,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.venue_add)) },
            )
        },
    ) { padding ->
        when {
            !state.isLoaded -> Unit

            state.isLibraryEmpty -> EmptyState(
                icon = CatIcons.Plain,
                title = stringResource(R.string.venues_empty_title),
                text = stringResource(R.string.venues_empty_text),
                modifier = Modifier.padding(padding),
            )

            state.venues.isEmpty() -> EmptyState(
                icon = CatIcons.Indifferent,
                title = stringResource(R.string.venues_nothing_found),
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.venues, key = { it.venue.id }) { summary ->
                    VenueCard(summary = summary, onClick = { onOpenVenue(summary.venue.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.venues_search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

@Composable
private fun SortMenu(current: VenueSort, onSelect: (VenueSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        VenueSort.NAME to R.string.sort_name,
        VenueSort.RATING to R.string.sort_rating,
        VenueSort.RECENT to R.string.sort_recent,
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

@Composable
private fun VenueCard(summary: VenueSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = summary.venue.name, style = MaterialTheme.typography.titleMedium)
                summary.venue.address?.takeIf { it.isNotBlank() }?.let { address ->
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.venue_counts,
                        summary.dishCount,
                        summary.drinkCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            RatingSummaryBadge(value = summary.effectiveRating)
        }
    }
}
