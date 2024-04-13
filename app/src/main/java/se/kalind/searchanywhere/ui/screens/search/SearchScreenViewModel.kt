package se.kalind.searchanywhere.ui.screens.search

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Looper
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
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
import se.kalind.searchanywhere.domain.repo.AppItem
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.SettingItem
import se.kalind.searchanywhere.domain.usecases.FilesUseCases
import se.kalind.searchanywhere.domain.usecases.GetAppsUseCase
import se.kalind.searchanywhere.domain.usecases.GetSettingsUseCase
import se.kalind.searchanywhere.domain.usecases.HistoryUseCases
import se.kalind.searchanywhere.domain.usecases.WeightedItem
import se.kalind.searchanywhere.ui.Loading
import se.kalind.searchanywhere.ui.PermissionStatusCallback
import se.kalind.searchanywhere.ui.SearchAnywhereFileProvider
import se.kalind.searchanywhere.ui.findMainActivity
import java.io.File
import javax.inject.Inject

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
    data class OpenDirectory(val item: FileItem) : ItemAction()
    data class DeleteFromHistory(val item: ItemType) : ItemAction()
}

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val getSettings: GetSettingsUseCase,
    private val getApps: GetAppsUseCase,
    private val files: FilesUseCases,
    private val history: HistoryUseCases,
) : ViewModel() {

    // Keeps track of wheter we are allowed to read/write files on external storage.
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
            getSettings.filteredSettings,
            getApps.filteredApps,
            files.filteredFiles,
            history.getHistory,
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

            val items = (appItems + settingItems + fileItems)
                .sortedByDescending { it.weight }
                .map { it.item }
                .toImmutableList()

            val histItems = Loading(history.map { it.toSearchItem() }.toImmutableList())

            UiState(
                items = items,
                history = histItems
            )
        }
            .flowOn(Dispatchers.Default)
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
        getSettings.setFilter(filter)
        getApps.setFilter(filter)
        if (filePermissionsGranted) {
            files.search(filter)
        }
    }

    fun onSearchFieldFocused(context: Context) {
        // context passed from Compose so we should always be able to get the MainActivity
        val activity = context.findMainActivity()!!

        activity.requestFilePermissions(object : PermissionStatusCallback {
            override fun onGranted() {
                Log.d("SearchAnywhere", "file permissions granted")
                files.createDatabaseIfNeeded()
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

    fun onItemAction(context: Context, action: ItemAction) {
        when (action) {
            is ItemAction.Open -> {
                openItem(context, action.item)
            }

            is ItemAction.OpenDirectory -> {
                history.saveToHistory(action.item.toItemType())
                openFile(context, action.item, openParentDir = true)
            }

            is ItemAction.DeleteFromHistory -> {
                history.deleteFromHistory(action.item)
            }
        }
    }

    private fun openItem(context: Context, item: ItemType) {
        when (item) {
            is ItemType.App -> {
                val app = item.item
                try {
                    val launchIntent =
                        context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                        history.saveToHistory(item)
                    } else {
                        Log.d("SearchAnywhere", "app unavailable")
                        val ret = _messages.tryEmit(Message("Could not start app", app))
                        Log.d("SearchAnywhere", "$ret")
                    }
                } catch (e: ActivityNotFoundException) {
                    Log.d("SearchAnywhere", "app unavailable")
                    val ret = _messages.tryEmit(Message("Could not start app", app))
                    Log.d("SearchAnywhere", "$ret")
                }
            }

            is ItemType.Setting -> {
                val setting = item.item
                try {
                    context.startActivity(Intent(setting.fieldValue))
                    history.saveToHistory(item)
                } catch (e: ActivityNotFoundException) {
                    Log.d("SearchAnywhere", "setting unavailable")
                    val ret = _messages.tryEmit(Message("Setting unavailable", item))
                    Log.d("SearchAnywhere", "$ret")
                }
            }

            is ItemType.File -> {
                history.saveToHistory(item)
                openFile(context, item.item)
            }
        }
    }

    private fun openFile(context: Context, fileItem: FileItem, openParentDir: Boolean = false) {
        val file =
            Environment.getExternalStorageDirectory().absolutePath + "/${fileItem.displayName}"

        Log.d("SearchAnywhere", "open file: $file")
        val fileObject = if (openParentDir) {
            File(file).parentFile ?: run {
                Log.w("SearchAnywhere", "$file did not have a valid parent")
                return
            }
        } else {
            File(file)
        }

        val uri = FileProvider.getUriForFile(
            context, SearchAnywhereFileProvider.AUTHORITY, fileObject
        )
        val type = if (openParentDir) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            SearchAnywhereFileProvider.getMimeType(file) ?: "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, type)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Define what your app should do if no activity can handle the intent.
            Log.d("SearchAnywhere", "failed to open file")
            val ret = _messages.tryEmit(Message("Couldn't open file", file))
            Log.d("SearchAnywhere", "$ret")
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
            icon = IconType.Drawable(this.icon),
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
