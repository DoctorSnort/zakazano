package kz.chaykin.zakazano.ui.venuelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.Biome
import kz.chaykin.zakazano.ui.components.ConfirmDialog

/** Какое окно сейчас открыто поверх меню биомов. */
private enum class BiomeDialog { NONE, CREATE, RENAME, DELETE }

/**
 * Заголовок главного экрана, он же переключатель биомов. Пока биом один, выглядит
 * почти как раньше — «Заказано» и стрелочка, чтобы не пугать тех, кому биомы не нужны.
 */
@Composable
fun BiomeSwitcher(
    biomes: List<Biome>,
    current: Biome?,
    onSelect: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf(BiomeDialog.NONE) }
    val several = biomes.size > 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { expanded = true },
    ) {
        Text(
            text = if (several && current != null) current.name else stringResource(R.string.app_name),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = stringResource(R.string.biome_switch),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        biomes.forEach { biome ->
            DropdownMenuItem(
                text = { Text(biome.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    if (biome.id == current?.id) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    } else {
                        // Пустое место того же размера, чтобы названия стояли ровным столбиком.
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Transparent)
                    }
                },
                trailingIcon = {
                    Text(
                        text = biome.venueCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                },
                onClick = {
                    expanded = false
                    onSelect(biome.id)
                },
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.biome_new)) },
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            onClick = {
                expanded = false
                dialog = BiomeDialog.CREATE
            },
        )
        if (current != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.biome_rename, current.name)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    dialog = BiomeDialog.RENAME
                },
            )
            // Последний биом не удаляется: главному экрану нужно хоть что-то показывать.
            if (several) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.biome_delete, current.name),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        expanded = false
                        dialog = BiomeDialog.DELETE
                    },
                )
            }
        }
    }

    when (dialog) {
        BiomeDialog.NONE -> Unit

        BiomeDialog.CREATE -> BiomeNameDialog(
            title = stringResource(R.string.biome_new),
            initial = "",
            confirmLabel = stringResource(R.string.biome_create),
            onConfirm = { name ->
                dialog = BiomeDialog.NONE
                onCreate(name)
            },
            onDismiss = { dialog = BiomeDialog.NONE },
        )

        BiomeDialog.RENAME -> if (current != null) {
            BiomeNameDialog(
                title = stringResource(R.string.biome_rename_title),
                initial = current.name,
                confirmLabel = stringResource(R.string.action_save),
                onConfirm = { name ->
                    dialog = BiomeDialog.NONE
                    onRename(current.id, name)
                },
                onDismiss = { dialog = BiomeDialog.NONE },
            )
        }

        BiomeDialog.DELETE -> if (current != null) {
            ConfirmDialog(
                title = stringResource(R.string.biome_delete_title, current.name),
                text = if (current.venueCount == 0) {
                    stringResource(R.string.biome_delete_text_empty)
                } else {
                    stringResource(R.string.biome_delete_text, current.venueCount)
                },
                onConfirm = {
                    dialog = BiomeDialog.NONE
                    onDelete(current.id)
                },
                onDismiss = { dialog = BiomeDialog.NONE },
            )
        }
    }
}

@Composable
private fun BiomeNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.biome_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                // Галочка на клавиатуре — то же, что кнопка: в поездке тянуться к ней некогда.
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
