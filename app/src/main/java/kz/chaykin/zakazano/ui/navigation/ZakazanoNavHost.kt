package kz.chaykin.zakazano.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kz.chaykin.zakazano.ui.itemeditor.ItemEditorScreen
import kz.chaykin.zakazano.ui.settings.SettingsScreen
import kz.chaykin.zakazano.ui.venuedetail.VenueDetailScreen
import kz.chaykin.zakazano.ui.venueeditor.VenueEditorScreen
import kz.chaykin.zakazano.ui.venuelist.VenueListScreen

@Composable
fun ZakazanoNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = VenueListRoute) {

        composable<VenueListRoute> {
            VenueListScreen(
                onOpenVenue = { venueId -> navController.navigate(VenueDetailRoute(venueId)) },
                onEditVenue = { venueId -> navController.navigate(VenueEditorRoute(venueId)) },
                onAddVenue = { navController.navigate(VenueEditorRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<VenueDetailRoute> {
            VenueDetailScreen(
                onBack = { navController.popBackStack() },
                onEditVenue = { venueId -> navController.navigate(VenueEditorRoute(venueId)) },
                onAddItem = { venueId, kind ->
                    navController.navigate(ItemEditorRoute(venueId = venueId, kind = kind))
                },
                onOpenItem = { venueId, itemId ->
                    navController.navigate(ItemEditorRoute(venueId = venueId, itemId = itemId))
                },
            )
        }

        composable<VenueEditorRoute> {
            VenueEditorScreen(
                onDone = { deleted ->
                    // После удаления возвращаться на экран удалённого заведения бессмысленно.
                    if (deleted) {
                        navController.popBackStack(VenueListRoute, inclusive = false)
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }

        composable<ItemEditorRoute> {
            ItemEditorScreen(onDone = { navController.popBackStack() })
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
