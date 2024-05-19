package se.kalind.searchanywhere.presentation.appbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.kalind.searchanywhere.presentation.R
import se.kalind.searchanywhere.presentation.components.FontScalePreviews
import se.kalind.searchanywhere.presentation.components.nonScaledSp
import se.kalind.searchanywhere.presentation.theme.AppTheme


@Composable
fun AppBottomBar(
    viewModel: AppBottomBarViewModel,
    expandSearchField: Boolean,
    onSettingsNavClicked: () -> Unit,
    onSearchNavClicked: () -> Unit,
) {
    val currentText by viewModel.currentSearchText.collectAsStateWithLifecycle()

    AppBottomBarContent(
        searchText = currentText,
        expandSearchField = expandSearchField,
        onSearchChanged = viewModel::onSearchChanged,
        onSearchReceivedFocus = viewModel::onSearchFieldFocused,
        onSettingsNavClicked = onSettingsNavClicked,
        onSearchNavClicked = onSearchNavClicked,
    )
}

@Composable
fun AppBottomBarContent(
    searchText: String,
    expandSearchField: Boolean,
    onSearchChanged: (String) -> Unit,
    onSearchReceivedFocus: () -> Unit,
    onSettingsNavClicked: () -> Unit,
    onSearchNavClicked: () -> Unit,
) {
    BottomAppBar {
        AnimatedContent(
            targetState = expandSearchField,
            label = "search field AnimatedContent",
            transitionSpec = {
                (slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left, animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            )
                        ) + fadeOut(animationSpec = tween(100))
                    )
            }
        ) { expandSearchField ->
            if (expandSearchField) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchTextField(
                        modifier = Modifier
                            .weight(3.5f)
                            .padding(bottom = 6.dp, start = 4.dp)
                            .requiredHeight(65.dp),
                        text = searchText,
                        onSearchChanged = onSearchChanged,
                        onReceivedFocus = onSearchReceivedFocus,
                    )
                    NavigationBarItem(
                        selected = false,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "settings icon"
                            )
                        },
                        onClick = onSettingsNavClicked
                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavigationBarItem(
                        selected = false,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "search icon"
                            )
                        },
                        label = {
                            Text("Search")
                        },
                        onClick = onSearchNavClicked
                    )
                    NavigationBarItem(
                        selected = true,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "settings icon"
                            )
                        },
                        label = {
                            Text("Settings")
                        },
                        onClick = onSettingsNavClicked
                    )
                }
            }
        }
    }
}

@Composable
fun SearchTextField(
    modifier: Modifier = Modifier,
    text: String,
    onSearchChanged: (String) -> Unit,
    onReceivedFocus: () -> Unit,
) {

    OutlinedTextField(
        modifier = modifier
            .onFocusChanged { focus ->
                if (focus.isFocused) {
                    onReceivedFocus()
                }
            },
        value = text,
        singleLine = true,
        textStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp.nonScaledSp,
        ),
        onValueChange = { new ->
            onSearchChanged(new)
        },
        label = { Text(stringResource(R.string.search_field_label)) },
        trailingIcon = {
            if (text.isEmpty()) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "search icon",
                )
            } else {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "clear text",
                    modifier = Modifier.clickable {
                        onSearchChanged("")
                    }
                )
            }
        },
    )
}

@FontScalePreviews
@Composable
fun AppBottomBarPreview(
) {
    AppTheme {
        AppBottomBarContent(
            searchText = "searching for this long string ",
            expandSearchField = true,
            onSearchReceivedFocus = {},
            onSearchChanged = {},
            onSettingsNavClicked = {},
            onSearchNavClicked = {},
        )
    }
}