package kz.chaykin.zakazano.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.ui.theme.ratingColors

/**
 * Выбор оценки: четыре крупные кнопки в ряд, попасть пальцем не глядя.
 * Ради этого и взят закрытый список вместо звёздочек.
 */
@Composable
fun RatingPicker(
    selected: Rating?,
    onSelect: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Rating.entries.forEach { rating ->
            val colors = MaterialTheme.ratingColors[rating]
            val isSelected = rating == selected
            val shape = RoundedCornerShape(12.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(if (isSelected) colors.container else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        border = if (isSelected) {
                            BorderStroke(2.dp, colors.accent)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        },
                        shape = shape,
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(rating) },
                    )
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            ) {
                Icon(
                    painter = painterResource(CatIcons.forRating(rating)),
                    contentDescription = null,
                    // Цвет держим на коте даже у невыбранной кнопки: иначе все четыре
                    // выглядят одинаково и выбирать приходится чтением, а не взглядом.
                    tint = if (isSelected) colors.onContainer else colors.accent,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = rating.label(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) colors.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Ряд фильтров по оценке: можно выбрать несколько, пустой выбор значит «показывать всё». */
@Composable
fun RatingFilterRow(
    selected: Set<Rating>,
    onToggle: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rating.entries.forEach { rating ->
            val colors = MaterialTheme.ratingColors[rating]
            val isOn = rating in selected
            Text(
                text = rating.label(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (isOn) colors.onContainer else colors.accent,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isOn) colors.container else MaterialTheme.colorScheme.surface)
                    .border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isOn) colors.accent else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        shape = RoundedCornerShape(50),
                    )
                    .selectable(
                        selected = isOn,
                        role = Role.Checkbox,
                        onClick = { onToggle(rating) },
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }
    }
}
