package kz.chaykin.zakazano.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kz.chaykin.zakazano.model.Rating

// Основная палитра — тёплая терракота: приложение про еду, а не про почту.
internal val LightPrimary = Color(0xFF8F4B1B)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFFFDBC8)
internal val LightOnPrimaryContainer = Color(0xFF331200)
internal val LightSecondary = Color(0xFF75574A)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFFFDBC8)
internal val LightOnSecondaryContainer = Color(0xFF2B160C)
internal val LightTertiary = Color(0xFF63612F)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFE9E6A7)
internal val LightOnTertiaryContainer = Color(0xFF1E1D00)
internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)
internal val LightBackground = Color(0xFFFFF8F6)
internal val LightOnBackground = Color(0xFF221A16)
internal val LightSurface = Color(0xFFFFF8F6)
internal val LightOnSurface = Color(0xFF221A16)
internal val LightSurfaceVariant = Color(0xFFF4DED4)
internal val LightOnSurfaceVariant = Color(0xFF52443D)
internal val LightOutline = Color(0xFF85736B)
internal val LightOutlineVariant = Color(0xFFD7C2B8)

// Оттенки поверхностей задаём явно: без них Material достраивает их нейтрально-серыми,
// и карточки выглядят холодными пятнами на тёплом фоне.
internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFFFF1EB)
internal val LightSurfaceContainer = Color(0xFFFCEBE3)
internal val LightSurfaceContainerHigh = Color(0xFFF7E5DD)
internal val LightSurfaceContainerHighest = Color(0xFFF1DFD8)
internal val LightSurfaceDim = Color(0xFFE6D6CE)
internal val LightSurfaceBright = Color(0xFFFFF8F6)
internal val LightInverseSurface = Color(0xFF382E2A)
internal val LightInverseOnSurface = Color(0xFFFFEDE6)
internal val LightInversePrimary = Color(0xFFFFB68C)

internal val DarkPrimary = Color(0xFFFFB68C)
internal val DarkOnPrimary = Color(0xFF542100)
internal val DarkPrimaryContainer = Color(0xFF723304)
internal val DarkOnPrimaryContainer = Color(0xFFFFDBC8)
internal val DarkSecondary = Color(0xFFE5BFAF)
internal val DarkOnSecondary = Color(0xFF432B20)
internal val DarkSecondaryContainer = Color(0xFF5C4035)
internal val DarkOnSecondaryContainer = Color(0xFFFFDBC8)
internal val DarkTertiary = Color(0xFFCDCA8D)
internal val DarkOnTertiary = Color(0xFF343205)
internal val DarkTertiaryContainer = Color(0xFF4B4919)
internal val DarkOnTertiaryContainer = Color(0xFFE9E6A7)
internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkBackground = Color(0xFF1A110D)
internal val DarkOnBackground = Color(0xFFF1DFD8)
internal val DarkSurface = Color(0xFF1A110D)
internal val DarkOnSurface = Color(0xFFF1DFD8)
internal val DarkSurfaceVariant = Color(0xFF52443D)
internal val DarkOnSurfaceVariant = Color(0xFFD7C2B8)
internal val DarkOutline = Color(0xFF9F8D84)
internal val DarkOutlineVariant = Color(0xFF52443D)

internal val DarkSurfaceContainerLowest = Color(0xFF140C08)
internal val DarkSurfaceContainerLow = Color(0xFF221A16)
internal val DarkSurfaceContainer = Color(0xFF271E19)
internal val DarkSurfaceContainerHigh = Color(0xFF322823)
internal val DarkSurfaceContainerHighest = Color(0xFF3D332E)
internal val DarkSurfaceDim = Color(0xFF1A110D)
internal val DarkSurfaceBright = Color(0xFF413731)
internal val DarkInverseSurface = Color(0xFFF1DFD8)
internal val DarkInverseOnSurface = Color(0xFF382E2A)
internal val DarkInversePrimary = Color(0xFF8F4B1B)

/**
 * Акцент для названия заведения. Неон в чистом виде годится только на тёмном фоне:
 * на светлой подложке он теряет контраст и текст становится нечитаемым, поэтому
 * в светлой теме берём густой зелёный, а свечение оставляем едва заметным.
 */
@Immutable
data class AccentColors(
    val venueTitle: Color,
    val venueGlow: Color,
)

internal val LightAccents = AccentColors(
    venueTitle = Color(0xFF0B7A3B),
    venueGlow = Color(0x4D2ECC71),
)

internal val DarkAccents = AccentColors(
    venueTitle = Color(0xFF5CFF9E),
    venueGlow = Color(0x9939FF7A),
)

/** Цвета одной оценки: для текста/иконки, для подложки бейджа и для текста на этой подложке. */
@Immutable
data class RatingColorSet(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

/**
 * Цвета оценок держим отдельно от схемы Material: это смысловые цвета, как «ошибка».
 * Они не должны уезжать вслед за обоями пользователя, иначе «стрём» однажды станет зелёным.
 */
@Immutable
data class RatingColors(
    val terrible: RatingColorSet,
    val meh: RatingColorSet,
    val good: RatingColorSet,
    val great: RatingColorSet,
) {
    operator fun get(rating: Rating): RatingColorSet = when (rating) {
        Rating.TERRIBLE -> terrible
        Rating.MEH -> meh
        Rating.GOOD -> good
        Rating.GREAT -> great
    }
}

internal val LightRatingColors = RatingColors(
    terrible = RatingColorSet(Color(0xFFB3261E), Color(0xFFFFDAD6), Color(0xFF410002)),
    meh = RatingColorSet(Color(0xFF5F6368), Color(0xFFE4E1E0), Color(0xFF1B1B1B)),
    good = RatingColorSet(Color(0xFF8A5A00), Color(0xFFFFDEA8), Color(0xFF2A1800)),
    great = RatingColorSet(Color(0xFF2E7D32), Color(0xFFC6EFC8), Color(0xFF052107)),
)

internal val DarkRatingColors = RatingColors(
    terrible = RatingColorSet(Color(0xFFFFB4AB), Color(0xFF8C1D18), Color(0xFFFFDAD6)),
    meh = RatingColorSet(Color(0xFFC4C7C5), Color(0xFF45484B), Color(0xFFE4E1E0)),
    good = RatingColorSet(Color(0xFFFFC46B), Color(0xFF6B4700), Color(0xFFFFDEA8)),
    great = RatingColorSet(Color(0xFF8BD98F), Color(0xFF1B5E20), Color(0xFFC6EFC8)),
)
