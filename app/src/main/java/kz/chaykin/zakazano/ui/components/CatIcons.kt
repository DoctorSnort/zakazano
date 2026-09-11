package kz.chaykin.zakazano.ui.components

import androidx.annotation.DrawableRes
import kz.chaykin.zakazano.R
import kz.chaykin.zakazano.model.Rating

/**
 * Коты вместо абстрактных пиктограмм: оценка «стрём» с зажмурившимся котом читается
 * быстрее, чем та же оценка с восклицательным знаком, и приложением приятнее пользоваться.
 *
 * Рисунки построены на иконке `cat` из Tabler Icons (MIT) — см. THIRD-PARTY.md.
 */
object CatIcons {

    @DrawableRes
    fun forRating(rating: Rating): Int = when (rating) {
        Rating.TERRIBLE -> R.drawable.ic_cat_terrible
        Rating.MEH -> R.drawable.ic_cat_meh
        Rating.GOOD -> R.drawable.ic_cat_good
        Rating.GREAT -> R.drawable.ic_cat_great
    }

    /** Обычный кот — заглушка вместо фотографии и пустой список заведений. */
    @DrawableRes
    val Plain: Int = R.drawable.ic_cat

    /** Кот над пустой миской — в заведении ещё ничего не записано. */
    @DrawableRes
    val EmptyBowl: Int = R.drawable.ic_cat_bowl

    /** Спящий кот — под фильтр ничего не подошло. */
    @DrawableRes
    val Sleeping: Int = R.drawable.ic_cat_sleep

    /** Равнодушный кот — поиск ничего не нашёл. */
    @DrawableRes
    val Indifferent: Int = R.drawable.ic_cat_meh
}
