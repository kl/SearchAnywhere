package se.kalind.searchanywhere.presentation.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.messageOrNull
import se.kalind.searchanywhere.domain.repo.AppItem
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.SettingItem
import se.kalind.searchanywhere.domain.usecases.AppsUseCase
import se.kalind.searchanywhere.domain.usecases.FilesUseCase
import se.kalind.searchanywhere.domain.usecases.HistoryUseCase
import se.kalind.searchanywhere.domain.usecases.OpenItemUseCase
import se.kalind.searchanywhere.domain.usecases.SettingsUseCase
import se.kalind.searchanywhere.domain.usecases.WeightedItem
import se.kalind.searchanywhere.presentation.Loading
import se.kalind.searchanywhere.presentation.asDrawable
import javax.inject.Inject
import javax.inject.Named

sealed class IconType {
    data class Vector(val icon: ImageVector) : IconType()
    data class Drawable(val icon: android.graphics.drawable.Drawable) : IconType()
}

data class SearchItem(
    val item: ItemType,
    val icon: IconType,
    val displayName: String,
    val key: String,
)

data class UiState(
    val items: ImmutableList<SearchItem>,
    val history: Loading<ImmutableList<SearchItem>>,
)

data class Message(val message: String, val key: Any)

sealed class ItemAction {
    data class Open(val item: ItemType) : ItemAction()
    data class DeleteFromHistory(val item: ItemType) : ItemAction()
}

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    ucSettings: SettingsUseCase,
    ucApps: AppsUseCase,
    ucFiles: FilesUseCase,
    private val ucHistory: HistoryUseCase,
    private val ucOpen: OpenItemUseCase,
    @Named("default") private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // Messages that are shown one time only in a snackbar
    private val _messages = MutableSharedFlow<Message>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<Message> = _messages

    // The main UI state
    val uiState: StateFlow<UiState> =
        combine(
            ucSettings.filteredSettings,
            ucApps.filteredApps,
            ucFiles.filteredFiles,
            ucHistory.getHistory,
        ) { settings, apps, files, history ->

            val settingItems = unwrapResultSequence(settings).map {
                WeightedItem(it.weight, it.item.toSearchItem())
            }
            val appItems = unwrapResultSequence(apps).map {
                WeightedItem(it.weight, it.item.toSearchItem())
            }
            val fileItems = unwrapResultSequence(files).map {
                WeightedItem(it.weight, it.item.toSearchItem())
            }

            val allItems: MutableList<WeightedItem<SearchItem>> = mutableListOf()
            allItems.addAll(settingItems)
            allItems.addAll(appItems)
            allItems.addAll(fileItems)
            allItems.sortByDescending { it.weight }
            val allImmutable = allItems.asSequence().map { it.item }.toImmutableList()

            val histItems =
                Loading(history.asSequence().map { it.toSearchItem() }.toImmutableList())

            UiState(
                items = allImmutable,
                history = histItems
            )
        }
            .flowOn(defaultDispatcher)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                UiState(
                    items = persistentListOf(),
                    history = Loading(null),
                ),
            )

    private fun <T> unwrapResultSequence(result: WorkResult<Sequence<T>>): Sequence<T> {
        return if (result is WorkResult.Success) {
            result.data
        } else {
            if (result is WorkResult.Error) {
                val message =
                    result.message ?: result.exception?.localizedMessage
                    ?: result.exception?.javaClass?.name
                if (message != null) {
                    _messages.tryEmit(Message(message, result))
                }
            }
            emptySequence()
        }
    }

    fun onItemAction(action: ItemAction) {
        when (action) {
            is ItemAction.Open -> {
                val result = ucOpen.openItem(action.item)
                result.messageOrNull()?.let {
                    _messages.tryEmit(Message(it, action.item))
                }
            }

            is ItemAction.DeleteFromHistory -> {
                ucHistory.deleteFromHistory(action.item)
            }
        }
    }
}

fun SettingItem.toSearchItem(): SearchItem {
    return SearchItem(
        item = ItemType.Setting(this),
        icon = IconType.Vector(Icons.Default.Settings),
        displayName = this.displayName,
        key = this.id,
    )
}

fun AppItem.toSearchItem(): SearchItem {
    return SearchItem(
        item = ItemType.App(this),
        icon = IconType.Drawable(this.icon.asDrawable()),
        displayName = this.displayName,
        key = this.id,
    )
}

fun FileItem.toSearchItem(): SearchItem {
    return SearchItem(
        item = ItemType.File(this),
        icon = IconType.Vector(Icons.Default.FileOpen),
        displayName = this.displayName,
        key = this.displayName,
    )
}

fun ItemType.toSearchItem(): SearchItem {
    return when (this) {
        is ItemType.App -> item.toSearchItem()
        is ItemType.Setting -> item.toSearchItem()
        is ItemType.File -> item.toSearchItem()
    }
}
