package se.kalind.searchanywhere.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import se.kalind.searchanywhere.presentation.appbar.AppBottomBar
import se.kalind.searchanywhere.presentation.appbar.AppBottomBarViewModel
import se.kalind.searchanywhere.presentation.config.SettingsScreen

@Composable
fun App(
    searchBarVm: AppBottomBarViewModel = hiltViewModel()
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
                    if (expandSearchField) {
                        expandSearchField = false
                        navController.navigateBarItem(SettingsRoute)
                    }
                },
                onSearchNavClicked = {
                    expandSearchField = true
                    navController.navigateBarItem(SearchRoute)
                },
            )
        }
    ) { insetsPadding ->

        NavHost(
            modifier = Modifier
                .padding(bottom = insetsPadding.calculateBottomPadding())
                .consumeWindowInsets(insetsPadding),
            navController = navController,
            startDestination = SearchRoute,
        ) {
            searchDestination(
                snackbarHostState = snackbarHostState,
                searchBarVm = searchBarVm
            )
            settingsDestination()
        }
    }
}

