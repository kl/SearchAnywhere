package se.kalind.searchanywhere.domain.usecases

import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import se.kalind.searchanywhere.domain.AppItemData
import se.kalind.searchanywhere.domain.AppsRepository
import se.kalind.searchanywhere.domain.WorkResult
import javax.inject.Inject

// The domain layer app item representation.
data class AppItem(
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
) : DisplayName {

    companion object {
        fun fromData(appData: AppItemData): AppItem {
            return AppItem(
                id = appData.id,
                label = appData.label,
                packageName = appData.packageName,
                activityName = appData.activityName,
                icon = appData.icon
            )
        }
    }

    override val displayName: String
        get() = label
}

typealias AppItems = List<WeightedItem<AppItem>>

class GetAppsUseCase @Inject constructor(appsRepository: AppsRepository) {

    private val _filter = MutableStateFlow("")

    val filteredApps: Flow<WorkResult<AppItems>> =
        appsRepository.availableApps().combine(_filter) { apps, filter ->
            val filtered = apps.map { appListData ->
                val appItems = appListData.map(AppItem::fromData)
                filterItems(appItems, filter)
            }
            Log.d("LOGZ", "emit filtered apps")
            filtered
        }

    fun setFilter(filter: String) {
        _filter.value = filter
    }
}

