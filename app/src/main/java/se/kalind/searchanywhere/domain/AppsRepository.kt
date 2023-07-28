package se.kalind.searchanywhere.domain

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.usecases.AppItem

interface AppsRepository {
    fun availableApps(): Flow<WorkResult<List<AppItemData>>>
    fun history(): Flow<List<Pair<AppItemData, UnixTimeMs>>>
    fun saveToHistory(item: AppItem)
}

// The app item data we expect the data layer to provide.
data class AppItemData(
    // Unique identifier
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
)
