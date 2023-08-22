package se.kalind.searchanywhere.ui.screens.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.ui.Loading
import se.kalind.searchanywhere.ui.theme.alegreyaFamily

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchScreenViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            onItemClick = { item -> viewModel.openItem(context, item) },
            onSearchChanged = { filter ->
                searchText.value = filter
                viewModel.onSearchChanged(searchText.value)
            },
        )
    }
}

@Composable
internal fun SearchScreenContent(
    items: List<SearchItem>,
    history: Loading<List<SearchItem>>,
    searchText: String,
    onItemClick: (ItemType) -> Unit,
    onSearchChanged: (String) -> Unit,
) {

    Column() {
        SearchTextField(
            text = searchText,
            onSearchChanged = onSearchChanged,
        )

        if (searchText.isEmpty() && items.isEmpty()) {
            val histItems = history.data
            if (!histItems.isNullOrEmpty()) {
                ItemList(
                    items = histItems,
                    onItemClick = onItemClick,
                    headerText = "History",
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
            ItemList(items = items, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun SearchTextField(
    text: String,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    items: List<SearchItem>,
    onItemClick: (ItemType) -> Unit,
    headerText: String? = null,
) {
    LazyColumn() {
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
            ItemRow(
                item = setting,
                onClick = onItemClick,
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemRow(
    item: SearchItem,
    onClick: (item: ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 4.dp),
            // .clickable modifier doesn't render the ripple correctly
            onClick = { onClick(item.item) },
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            val icon = item.icon
            when (icon) {
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
}
