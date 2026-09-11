package kz.chaykin.zakazano.ui.navigation

import kotlinx.serialization.Serializable
import kz.chaykin.zakazano.model.ItemKind

/**
 * Маршруты типобезопасной навигации. Идентификаторы всегда Long, где 0 значит «ещё не сохранено»:
 * nullable-параметры в маршрутах требуют своего NavType и ничего не дают взамен.
 */
@Serializable
data object VenueListRoute

@Serializable
data class VenueDetailRoute(val venueId: Long)

@Serializable
data class VenueEditorRoute(val venueId: Long = 0L)

@Serializable
data class ItemEditorRoute(
    val venueId: Long,
    val itemId: Long = 0L,
    val kind: ItemKind = ItemKind.DISH,
)

@Serializable
data object SettingsRoute
