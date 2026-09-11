package kz.chaykin.zakazano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.ui.theme.ratingColors

@Composable
fun Rating.label(): String = stringResource(
    when (this) {
        Rating.TERRIBLE -> R.string.rating_terrible
        Rating.MEH -> R.string.rating_meh
        Rating.GOOD -> R.string.rating_good
        Rating.GREAT -> R.string.rating_great
    },
)

/**
 * Бейдж оценки. Цвет плюс подпись, всегда вместе: по одному цвету список нечитаем
 * при дальтонизме и на солнце, а по одной подписи — не сканируется взглядом.
 */
@Composable
fun RatingBadge(
    rating: Rating,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.ratingColors[rating]
    Text(
        text = rating.label(),
        style = MaterialTheme.typography.labelLarge,
        color = colors.onContainer,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
