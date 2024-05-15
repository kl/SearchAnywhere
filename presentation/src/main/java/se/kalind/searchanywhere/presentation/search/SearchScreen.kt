package se.kalind.searchanywhere.presentation.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionMode
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.presentation.Loading
import se.kalind.searchanywhere.presentation.R
import se.kalind.searchanywhere.presentation.appbar.AppBottomBarViewModel
import se.kalind.searchanywhere.presentation.components.LongPressCard
import se.kalind.searchanywhere.presentation.theme.alegreyaFamily

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchScreenViewModel,
    searchBarVm: AppBottomBarViewModel,
    snackbarHostState: SnackbarHostState,
    insetsPadding: PaddingValues,
) {
    val message by viewModel.messages.collectAsStateWithLifecycle(
        initialValue = Message(
            "",
            0
        )
    )

    if (message.message.isNotEmpty()) {
        LaunchedEffect(snackbarHostState) {
            snackbarHostState.showSnackbar(message.message)
        }
    }

    val showPermissionRationale by searchBarVm.showPermissionRationale.collectAsStateWithLifecycle()

    if (showPermissionRationale != null) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale?.invoke() },
            title = { Text(stringResource(R.string.storage_permissions_reson_header)) },
            text = {
                Text(stringResource(R.string.storage_permission_text))
            },
            confirmButton = {
                TextButton(onClick = { showPermissionRationale?.invoke() }) {
                    Text("ok".uppercase())
                }
            },
        )
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSearch by searchBarVm.currentSearchText.collectAsStateWithLifecycle()

    SearchScreenContent(
        modifier = modifier.padding(start = 16.dp, end = 16.dp),
        insetsPadding = insetsPadding,
        items = state.items,
        history = state.history,
        searchText = currentSearch,
        onItemAction = { item -> viewModel.onItemAction(item) },
    )
}

@Composable
internal fun SearchScreenContent(
    modifier: Modifier = Modifier,
    insetsPadding: PaddingValues,
    items: ImmutableList<SearchItem>,
    history: Loading<ImmutableList<SearchItem>>,
    searchText: String,
    onItemAction: (ItemAction) -> Unit,
) {

    Box(modifier = modifier) {

        if (searchText.isEmpty()) {
            val histItems = history.data
            if (!histItems.isNullOrEmpty()) {
                ItemList(
                    items = histItems,
                    insetsPadding = insetsPadding,
                    onItemAction = onItemAction,
                    headerText = "History",
                    isHistory = true,
                )
            } else {
                if (history.hasLoaded()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_history_available),
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontFamily = alegreyaFamily,
                            lineHeight = 30.sp,
                        )
                    }
                }
            }
        } else {
            ItemList(
                items = items,
                insetsPadding = insetsPadding,
                onItemAction = onItemAction
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    items: ImmutableList<SearchItem>,
    insetsPadding: PaddingValues,
    onItemAction: (ItemAction) -> Unit,
    headerText: String? = null,
    isHistory: Boolean = false,
) {
    val listState = rememberLazyListState()

    LazyColumnScrollbar(
        listState,
        modifier = Modifier.padding(bottom = insetsPadding.calculateBottomPadding()),
        selectionMode = ScrollbarSelectionMode.Full,
        alwaysShowScrollBar = false,
        hideDelayMillis = 1200,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = insetsPadding.calculateTopPadding())
        ) {
            item {
                Spacer(modifier = Modifier.padding(top = 10.dp))
            }
            if (headerText != null) {
                item {
                    Text(
                        text = headerText,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontFamily = alegreyaFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    )
                }
            }
            items(
                items = items,
                key = { it.key },
            ) { item ->
                ItemCard(
                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    onItemAction = onItemAction,
                    isHistory = isHistory,
                    listState = listState,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    modifier: Modifier = Modifier,
    item: SearchItem,
    listState: LazyListState,
    isHistory: Boolean = false,
    onItemAction: (item: ItemAction) -> Unit,
) {
    var isContextMenuVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var pressOffset by remember {
        mutableStateOf(DpOffset.Zero)
    }
    var itemHeight by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    LongPressCard(
        modifier = modifier
            .padding(bottom = 8.dp)
            .clip(CardDefaults.shape)
            .onSizeChanged {
                itemHeight = with(density) { it.height.toDp() }
            },
        listState = listState,
        onTap = { onItemAction(ItemAction.Open(item.item)) },
        onLongPress = { offset ->
            pressOffset = with(density) {
                DpOffset(offset.x.toDp(), offset.y.toDp())
            }
            isContextMenuVisible = true
        },
    ) {

        ItemCardContent(item)
        DropdownMenu(
            expanded = isContextMenuVisible,
            onDismissRequest = { isContextMenuVisible = false },
            offset = pressOffset.copy(
                y = pressOffset.y - itemHeight
            )
        ) {
            DropdownItems(item.item, isHistory) { action ->
                isContextMenuVisible = false
                onItemAction(action)
            }
        }
    }
}

@Composable
fun DropdownItems(item: ItemType, isHistory: Boolean, onItemAction: (ItemAction) -> Unit) {
    if (isHistory) {
        DropdownMenuItem(
            text = { Text("Remove from history") },
            onClick = { onItemAction(ItemAction.DeleteFromHistory(item)) })
    }
}

@Composable
private fun ItemCardContent(
    item: SearchItem,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
            .fillMaxWidth()
    ) {
        when (val icon = item.icon) {
            is IconType.Vector -> {
                Icon(
                    icon.icon,
                    contentDescription = "item icon",
                    modifier = Modifier.size(28.dp)
                )
            }

            is IconType.Drawable -> {
                Image(
                    painter = rememberDrawablePainter(drawable = icon.icon),
                    contentDescription = "item icon",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(text = item.displayName)
    }
}
