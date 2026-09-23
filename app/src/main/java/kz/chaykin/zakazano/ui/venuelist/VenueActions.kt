package kz.chaykin.zakazano.ui.venuelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.Biome
import kz.chaykin.zakazano.model.Venue
import kz.chaykin.zakazano.ui.components.ConfirmDialog

/** Шаг внутри окна действий: сам список действий, выбор биома или подтверждение удаления. */
private enum class ActionStep { MENU, MOVE, DELETE }

/**
 * Действия с заведением прямо из списка — по долгому нажатию на карточку.
 * Раньше за тем же приходилось заходить внутрь и искать меню там.
 */
@Composable
fun VenueActionsDialog(
    venue: Venue,
    /** Биомы, куда можно перенести: все, кроме того, где заведение лежит сейчас. */
    otherBiomes: List<Biome>,
    onEdit: () -> Unit,
    onMove: (biomeId: Long) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(ActionStep.MENU) }

    when (step) {
        ActionStep.MENU -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(venue.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    ActionRow(Icons.Outlined.Edit, stringResource(R.string.venue_action_edit), onClick = onEdit)
                    if (otherBiomes.isNotEmpty()) {
                        ActionRow(
                            icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                            label = stringResource(R.string.venue_action_move),
                            onClick = { step = ActionStep.MOVE },
                        )
                    }
                    ActionRow(
                        icon = Icons.Outlined.Delete,
                        label = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { step = ActionStep.DELETE },
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
        )

        ActionStep.MOVE -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.venue_move_title, venue.name)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    otherBiomes.forEach { biome ->
                        ActionRow(
                            icon = null,
                            label = biome.name,
                            onClick = { onMove(biome.id) },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { step = ActionStep.MENU }) { Text(stringResource(R.string.action_back)) }
            },
        )

        ActionStep.DELETE -> ConfirmDialog(
            title = stringResource(R.string.venue_delete_title),
            text = stringResource(R.string.venue_delete_text, venue.name),
            onConfirm = onDelete,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector?,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}
