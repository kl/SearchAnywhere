package se.kalind.searchanywhere.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import se.kalind.searchanywhere.presentation.appbar.AppBottomBarViewModel
import se.kalind.searchanywhere.presentation.config.SettingsScreen
import se.kalind.searchanywhere.presentation.search.SearchScreen

sealed interface Route

@Serializable
data object SearchRoute : Route

@Serializable
data object SettingsRoute : Route

fun NavHostController.navigateBarItem(to: Route) {
    navigate(to) {
        launchSingleTop = true
        restoreState = true

        popUpTo(graph.findStartDestination().id) {
            inclusive = true
            saveState = true
        }
    }
}

fun NavGraphBuilder.searchDestination(
    snackbarHostState: SnackbarHostState,
    searchBarVm: AppBottomBarViewModel
) {
    composable<SearchRoute> {
        SearchScreen(
            viewModel = hiltViewModel(),
            searchBarVm = searchBarVm,
            snackbarHostState = snackbarHostState,
        )
    }
}

fun NavGraphBuilder.settingsDestination() {
    composable<SettingsRoute> {
        SettingsScreen(
            viewModel = hiltViewModel(),
        )
    }
}
