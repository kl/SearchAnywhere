package se.kalind.searchanywhere.presentation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import se.kalind.searchanywhere.presentation.appbar.AppBottomBar
import se.kalind.searchanywhere.presentation.appbar.AppBottomBarViewModel
import se.kalind.searchanywhere.presentation.search.SearchScreen
import se.kalind.searchanywhere.presentation.search.SearchScreenViewModel
import se.kalind.searchanywhere.presentation.settings.SettingsScreen

class Routes {
    companion object {
        const val SEARCH = "search"
        const val SETTINGS = "settings"
    }
}

@Composable
fun App(
    searchScreenViewModel: SearchScreenViewModel = hiltViewModel(),
    searchBarVm: AppBottomBarViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry?.destination?.route ?: Routes.SEARCH

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
        bottomBar = {
            AppBottomBar(
                viewModel = searchBarVm,
                onSettingsNavClicked = {
                    navController.navigateBarItem(Routes.SETTINGS)
                },
                onSearchNavClicked = {
                    navController.navigateBarItem(Routes.SEARCH)
                },
            )
        }
    ) { insetsPadding ->

        NavHost(
            modifier = Modifier.consumeWindowInsets(insetsPadding),
            navController = navController,
            startDestination = currentRoute
        ) {
            composable(Routes.SEARCH) {
                SearchScreen(
                    viewModel = searchScreenViewModel,
                    searchBarVm = searchBarVm,
                    snackbarHostState = snackbarHostState,
                    insetsPadding = insetsPadding,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(insetsPadding)
            }
        }
    }
}

fun NavHostController.navigateBarItem(to: String) {

    navigate(to) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
            saveState = true
        }
        launchSingleTop = true
    }
}
