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
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import se.kalind.searchanywhere.presentation.appbar.AppBottomBar
import se.kalind.searchanywhere.presentation.appbar.AppBottomBarViewModel
import se.kalind.searchanywhere.presentation.config.SettingsScreen
import se.kalind.searchanywhere.presentation.search.SearchScreen
import se.kalind.searchanywhere.presentation.search.SearchScreenViewModel

@Composable
fun App(
    searchScreenViewModel: SearchScreenViewModel = hiltViewModel(),
    searchBarVm: AppBottomBarViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }

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
                    navController.navigateBarItem(SettingsRoute)
                },
                onSearchNavClicked = {
                    expandSearchField = true
                    navController.navigateBarItem(SearchRoute)
                },
            )
        }
    ) { insetsPadding ->

        NavHost(
            modifier = Modifier.consumeWindowInsets(insetsPadding),
            navController = navController,
            startDestination = SearchRoute,
        ) {
            composable<SearchRoute> {
                SearchScreen(
                    viewModel = searchScreenViewModel,
                    searchBarVm = searchBarVm,
                    snackbarHostState = snackbarHostState,
                    insetsPadding = insetsPadding,
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(insetsPadding = insetsPadding)
            }
        }
    }
}

fun <T : Any> NavHostController.navigateBarItem(to: T) {

    navigate(to) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
            saveState = true
        }
        launchSingleTop = true
    }
}

@Serializable
object SearchRoute

@Serializable
object SettingsRoute
