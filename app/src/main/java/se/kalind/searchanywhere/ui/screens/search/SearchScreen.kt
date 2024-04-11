package se.kalind.searchanywhere.ui.screens.search

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionMode
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.ui.Loading
import se.kalind.searchanywhere.ui.components.LongPressCard
import se.kalind.searchanywhere.ui.theme.alegreyaFamily

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchScreenViewModel = hiltViewModel()
) {
    val message by viewModel.messages.collectAsStateWithLifecycle(
        initialValue = Message(
            "",
            0
        )
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (message.message.isNotEmpty()) {
        scope.launch {
            snackbarHostState.showSnackbar(message.message)
        }
    }

    val showPermissionRationale by viewModel.showPermissionRationale.collectAsStateWithLifecycle()

    if (showPermissionRationale != null) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale?.invoke() },
            title = { Text("Why this app requests external storage permissions") },
            text = {
                Text(
                    "Granting storage read/write permissions lets you search for files " +
                            "on the external storage. The write permission is needed to create " +
                            "the file cache database which enables fast file searches. " +
                            "If you do not grant this permission the app will not find files but " +
                            "will still find Apps and Settings."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionRationale?.invoke() }) {
                    Text("ok".uppercase())
                }
            },
        )
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val searchText = rememberSaveable { mutableStateOf("") }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)
    ) { padding ->
        val context = LocalContext.current
        SearchScreenContent(
            items = state.items,
            history = state.history,
            searchText = searchText.value,
            onItemAction = { item -> viewModel.onItemAction(context, item) },
            onSearchChanged = { filter ->
                searchText.value = filter
                viewModel.onSearchChanged(searchText.value)
            },
            onSearchFieldFocused = { viewModel.onSearchFieldFocused(context) },
        )
    }
}

@Composable
internal fun SearchScreenContent(
    items: ImmutableList<SearchItem>,
    history: Loading<ImmutableList<SearchItem>>,
    searchText: String,
    onItemAction: (ItemAction) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSearchFieldFocused: () -> Unit,
) {

    Column {
        SearchTextField(
            text = searchText,
            onSearchChanged = onSearchChanged,
            onReceivedFocus = onSearchFieldFocused,
        )

        if (searchText.isEmpty() && items.isEmpty()) {
            val histItems = history.data
            if (!histItems.isNullOrEmpty()) {
                ItemList(
                    items = histItems,
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
                            "No History Available\nTry Doing A Search",
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontFamily = alegreyaFamily,
                            lineHeight = 30.sp,
                        )
                    }
                }
            }
        } else {
            ItemList(items = items, onItemAction = onItemAction)
        }
    }
}

@Composable
private fun SearchTextField(
    text: String,
    onSearchChanged: (String) -> Unit,
    onReceivedFocus: () -> Unit,
) {

    OutlinedTextField(
        value = text,
        singleLine = true,
        textStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        ),
        onValueChange = { new ->
            onSearchChanged(new)
        },
        label = { Text("Search Anywhere") },
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
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focus ->
                if (focus.isFocused) {
                    onReceivedFocus()
                }
            }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    items: ImmutableList<SearchItem>,
    onItemAction: (ItemAction) -> Unit,
    headerText: String? = null,
    isHistory: Boolean = false,
) {
    val listState = rememberLazyListState()

    LazyColumnScrollbar(
        listState,
        selectionMode = ScrollbarSelectionMode.Full,
        alwaysShowScrollBar = false,
        hideDelayMillis = 1200,
    ) {
        LazyColumn(state = listState) {
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
                            .padding(top = 10.dp, bottom = 5.dp),
                    )
                }
            }
            items(
                items = items,
                key = { it.key },
            ) { setting ->
                ItemCard(
                    modifier = Modifier.animateItemPlacement(),
                    item = setting,
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
            .padding(vertical = 4.dp, horizontal = 4.dp)
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
            text = { Text("Delete from history") },
            onClick = { onItemAction(ItemAction.DeleteFromHistory(item)) })
    }
    if (item is ItemType.File) {
        DropdownMenuItem(
            text = { Text("Open folder") },
            onClick = { onItemAction(ItemAction.OpenDirectory(item.item)) })
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
