package se.kalind.searchanywhere.ui.screens.search

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import se.kalind.searchanywhere.domain.usecases.AppItem
import se.kalind.searchanywhere.domain.usecases.FileItem
import se.kalind.searchanywhere.domain.usecases.FilesUseCases
import se.kalind.searchanywhere.domain.usecases.GetAppsUseCase
import se.kalind.searchanywhere.domain.usecases.GetSettingsUseCase
import se.kalind.searchanywhere.domain.usecases.HistoryUseCases
import se.kalind.searchanywhere.domain.usecases.SettingItem
import se.kalind.searchanywhere.domain.usecases.WeightedItem
import se.kalind.searchanywhere.ui.Loading
import se.kalind.searchanywhere.ui.findMainActivity
import javax.inject.Inject

sealed class IconType {
    data class Vector(val icon: ImageVector) : IconType()
    data class Drawable(val icon: android.graphics.drawable.Drawable) : IconType()
}

data class SearchItem(
    val item: ItemType,
    val icon: IconType,
    val displayName: String,
    val key: Any,
)

data class UiState(
    val items: List<SearchItem>,
    val history: Loading<List<SearchItem>>,
)

data class Message(val message: String, val key: Any)

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val getSettings: GetSettingsUseCase,
    private val getApps: GetAppsUseCase,
    private val files: FilesUseCases,
    private val history: HistoryUseCases,
) : ViewModel() {

    private var permissionsGranted = false

//    init {
//        files.rebuildDatabase(ScanRoot.INTERNAL)
//    }

//    private val files: MutableStateFlow<ArrayList<String>> = MutableStateFlow(ArrayList())

    private val _messages = MutableSharedFlow<Message>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val messages: SharedFlow<Message> = _messages

    val uiState: StateFlow<UiState> =
        combine(
            getSettings.filteredSettings,
            getApps.filteredApps,
            files.filteredFiles,
            history.getHistory,
        ) { settings, apps, files, history ->

            val settingItems = unwrapResultList(settings).map {
                WeightedItem(it.weight, it.item.toSearchItem())
            }
            val appItems = unwrapResultList(apps).map {
                WeightedItem(it.weight, it.item.toSearchItem())
            }
            val fileItems = unwrapResultList(files).map {
                WeightedItem(it.weight, it.item.toSearchItem())
            }

            val items = (appItems + settingItems + fileItems)
                .sortedByDescending { it.weight }
                .map { it.item }

            val histItems = Loading(history.map(ItemType::toSearchItem))

            UiState(
                items = items,
                history = histItems
            )
        }
            .flowOn(Dispatchers.IO)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                UiState(
                    items = emptyList(),
                    history = Loading(null),
                ),
            )

    private fun <T> unwrapResultList(result: WorkResult<List<T>>): List<T> {
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
            emptyList()
        }
    }

    fun onSearchChanged(filter: String) {
        getSettings.setFilter(filter)
        getApps.setFilter(filter)
        files.setFilter(filter)
    }

    fun onSearchFieldFocused(context: Context) {
        // context passed from Compose so we should always be able to get the MainActivity
        val activity = context.findMainActivity()!!
        Log.d("LOGZ", "FOCUSED: true")

        activity.requestFilePermissions { granted ->
            Log.d("LOGZ", "permissions granted: $granted (viewmodel)")
            if (granted) {
                files.createDatabaseIfNeeded()
            }
        }
    }

    fun openItem(context: Context, item: ItemType) {
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
                        Log.d("LOGZ", "app unavailable")
                        val ret = _messages.tryEmit(Message("Could not start app", app))
                        Log.d("LOGZ", "$ret")
                    }
                } catch (e: ActivityNotFoundException) {
                    Log.d("LOGZ", "app unavailable")
                    val ret = _messages.tryEmit(Message("Could not start app", app))
                    Log.d("LOGZ", "$ret")
                }
            }

            is ItemType.Setting -> {
                val setting = item.item
                try {
                    context.startActivity(Intent(setting.fieldValue))
                    history.saveToHistory(item)
                } catch (e: ActivityNotFoundException) {
                    Log.d("LOGZ", "setting unavailable")
                    val ret = _messages.tryEmit(Message("Setting unavailable", item))
                    Log.d("LOGZ", "$ret")
                }
            }

            is ItemType.File -> {
                Log.d("LOGZ", "open file: ${item.item.displayName}")
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
        icon = IconType.Drawable(this.icon),
        displayName = this.displayName,
        key = this.id,
    )
}

fun FileItem.toSearchItem(): SearchItem {
    return SearchItem(
        item = ItemType.File(this),
        icon = IconType.Vector(Icons.Default.MailOutline),
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
