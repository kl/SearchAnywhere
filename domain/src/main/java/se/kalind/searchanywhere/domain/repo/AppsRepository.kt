package se.kalind.searchanywhere.domain.repo

import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.ToItemType
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.DisplayName

interface AppsRepository {
    fun availableApps(): Flow<WorkResult<List<AppItemData>>>
    fun history(): Flow<List<Pair<AppItemData, UnixTimeMs>>>
    fun saveToHistory(item: AppItem)
    fun deleteFromHistory(item: AppItem)
}

class AppIconDrawable(val icon: Any)

// The app item data we expect the data layer to provide.
data class AppItemData(
    // Unique identifier
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: AppIconDrawable,
) : ToItemType {
    override fun toItemType(): ItemType {
        return AppItem.fromData(this).toItemType()
    }
}

// The domain layer app item representation.
data class AppItem(
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: AppIconDrawable,
) : DisplayName, ToItemType {

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

    override fun toItemType(): ItemType {
        return ItemType.App(this)
    }
}
