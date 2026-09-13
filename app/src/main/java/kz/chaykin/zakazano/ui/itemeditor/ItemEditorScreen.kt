package kz.chaykin.zakazano.ui.itemeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.data.photo.PhotoStore
import kz.chaykin.zakazano.model.DrinkType
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Photo
import kz.chaykin.zakazano.ui.components.label
import kz.chaykin.zakazano.ui.components.ConfirmDialog
import kz.chaykin.zakazano.ui.components.rememberCameraCapture
import kz.chaykin.zakazano.ui.components.PhotoViewerDialog
import kz.chaykin.zakazano.ui.components.RatingPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorScreen(
    onDone: () -> Unit,
    viewModel: ItemEditorViewModel = viewModel(factory = ItemEditorViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    val cameraUnavailable = stringResource(R.string.photo_camera_unavailable)

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.addPhotoFromGallery(uri)
    }

    val takePhoto = rememberCameraCapture(
        createTarget = viewModel::newCameraTarget,
        onCaptured = viewModel::addPhotoFromCamera,
        onUnavailable = { scope.launch { snackbarHost.showSnackbar(cameraUnavailable) } },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when {
                                !state.isNew -> R.string.item_edit
                                state.kind == ItemKind.DISH -> R.string.item_new_dish
                                else -> R.string.item_new_drink
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(onClick = viewModel::save) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Без этого клавиатура накрывает поле, в котором как раз и печатают.
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KindSelector(selected = state.kind, onSelect = viewModel::onKindChange)

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.item_name)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.kind == ItemKind.DRINK) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.drink_type), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrinkType.entries.forEach { type ->
                            FilterChip(
                                selected = state.drinkType == type,
                                onClick = { viewModel.onDrinkTypeChange(type) },
                                label = { Text(type.label()) },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.item_rating), style = MaterialTheme.typography.labelLarge)
                RatingPicker(selected = state.rating, onSelect = viewModel::onRatingChange)
            }

            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.item_price)) },
                suffix = { Text(state.currency.symbol) },
                isError = state.priceError,
                supportingText = if (state.priceError) {
                    { Text(stringResource(R.string.error_price_invalid)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.comment,
                onValueChange = viewModel::onCommentChange,
                label = { Text(stringResource(R.string.item_comment)) },
                placeholder = { Text(stringResource(R.string.item_comment_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            PhotoSection(
                photos = state.photos,
                onRemove = viewModel::removePhoto,
                onOpen = { index -> viewerIndex = index },
                onPickGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onTakePhoto = takePhoto,
            )
        }
    }

    viewerIndex?.let { index ->
        PhotoViewerDialog(
            photos = state.photos,
            startIndex = index,
            onDismiss = { viewerIndex = null },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.item_delete_title),
            text = stringResource(R.string.item_delete_text, state.name),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindSelector(selected: ItemKind, onSelect: (ItemKind) -> Unit) {
    val options = listOf(ItemKind.DISH to R.string.tab_dishes, ItemKind.DRINK to R.string.tab_drinks)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (kind, labelRes) ->
            SegmentedButton(
                selected = kind == selected,
                onClick = { onSelect(kind) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(stringResource(labelRes))
            }
        }
    }
}

@Composable
private fun PhotoSection(
    photos: List<Photo>,
    onRemove: (Photo) -> Unit,
    onOpen: (Int) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.photos), style = MaterialTheme.typography.labelLarge)

        if (photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(photos, key = { _, photo -> photo.fileName }) { index, photo ->
                    Box {
                        AsyncImage(
                            model = PhotoStore.fileIn(context, photo.fileName),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onOpen(index) },
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onRemove(photo) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.photo_remove),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.photo_camera),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = onPickGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.photo_gallery),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
