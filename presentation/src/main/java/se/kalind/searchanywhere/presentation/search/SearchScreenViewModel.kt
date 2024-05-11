package se.kalind.searchanywhere.presentation.search

import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
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
import se.kalind.searchanywhere.presentation.MainActivityReference
import se.kalind.searchanywhere.presentation.PermissionStatusCallback
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
    private val ucSettings: SettingsUseCase,
    private val ucApps: AppsUseCase,
    private val ucFiles: FilesUseCase,
    private val ucHistory: HistoryUseCase,
    private val ucOpen: OpenItemUseCase,
    private val mainActivityRef: MainActivityReference,
    @Named("default") private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // Keeps track of whether we are allowed to read/write files on external storage.
    // If this is false we do not try to search the file database.
    private var filePermissionsGranted = false

    // Messages that are shown one time only in a snackbar
    private val _messages = MutableSharedFlow<Message>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<Message> = _messages

    // When set to a non-null value a dialog is shown explaining why we ask for file permissions.
    private val _showPermissionRationale: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)
    val showPermissionRationale: StateFlow<(() -> Unit)?> = _showPermissionRationale

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

            val histItems = Loading(history.asSequence().map { it.toSearchItem() }.toImmutableList())

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

    fun onSearchChanged(filter: String) {
        ucSettings.setFilter(filter)
        ucApps.setFilter(filter)
        if (filePermissionsGranted) {
            ucFiles.search(filter)
        }
    }

    fun onSearchFieldFocused() {
        val activity = mainActivityRef.mainActivity ?: return

        activity.requestFilePermissions(object : PermissionStatusCallback {
            override fun onGranted() {
                Log.d("SearchAnywhere", "file permissions granted")
                ucFiles.createDatabaseIfNeeded()
                filePermissionsGranted = true
            }

            override fun onDenied() {
                Log.d("SearchAnywhere", "file permissions denied")
                filePermissionsGranted = false
            }

            override fun onShowRationale(afterShown: () -> Unit) {
                // The callback in _showPermissionRationale is called from Compose after the user
                // has clicked away the rationale dialog. afterShown() will then trigger another
                // permissions request which will call onGranted() or onDenied() on this object.
                _showPermissionRationale.value = {
                    afterShown()
                    _showPermissionRationale.value = null
                }
            }
        })
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
}
