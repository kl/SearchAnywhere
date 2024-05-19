package se.kalind.searchanywhere.presentation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import se.kalind.searchanywhere.presentation.config.SettingsScreen

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

    var expandSearchField by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
        bottomBar = {
            AppBottomBar(
                viewModel = searchBarVm,
                expandSearchField = expandSearchField,
                onSettingsNavClicked = {
                    expandSearchField = false
                    navController.navigateBarItem(Routes.SETTINGS)
                },
                onSearchNavClicked = {
                    expandSearchField = true
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
                SettingsScreen(insetsPadding = insetsPadding)
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
