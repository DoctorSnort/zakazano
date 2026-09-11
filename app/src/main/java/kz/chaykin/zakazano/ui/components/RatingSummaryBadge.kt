package kz.chaykin.zakazano.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.Rating
import kotlin.math.roundToInt

/**
 * Средняя оценка показывается той же подписью, что и обычная: «в среднем 2.4» человеку
 * ничего не говорит, а «неплохо» — говорит.
 */
@Composable
fun RatingSummaryBadge(value: Double?, modifier: Modifier = Modifier) {
    if (value == null) {
        Text(
            text = stringResource(R.string.venue_no_ratings),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = modifier,
        )
    } else {
        val rounded = value.roundToInt().coerceIn(Rating.TERRIBLE.code, Rating.GREAT.code)
        RatingBadge(rating = Rating.fromCode(rounded), modifier = modifier)
    }
}
