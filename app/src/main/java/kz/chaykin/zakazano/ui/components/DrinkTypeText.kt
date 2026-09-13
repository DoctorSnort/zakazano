package kz.chaykin.zakazano.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.DrinkType
import kz.chaykin.zakazano.ui.theme.accentColors

@Composable
fun DrinkType.label(): String = stringResource(
    when (this) {
        DrinkType.COCKTAIL -> R.string.drink_type_cocktail
        DrinkType.BEER -> R.string.drink_type_beer
        DrinkType.TINCTURE -> R.string.drink_type_tincture
    },
)

/**
 * Название позиции, а следом вид напитка в скобках — курсивом и холодным цветом,
 * чтобы он читался как пометка, а не как часть названия.
 */
@Composable
fun itemTitle(name: String, drinkType: DrinkType?): AnnotatedString {
    if (drinkType == null) return AnnotatedString(name)

    val typeText = drinkType.label()
    val typeColor = MaterialTheme.accentColors.drinkType
    return buildAnnotatedString {
        append(name)
        withStyle(
            SpanStyle(
                color = typeColor,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
            ),
        ) {
            append("  ($typeText)")
        }
    }
}
