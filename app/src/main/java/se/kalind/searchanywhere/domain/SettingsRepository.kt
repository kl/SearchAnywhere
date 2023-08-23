package se.kalind.searchanywhere.domain

import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.usecases.SettingItem

interface SettingsRepository {
    fun availableSettings(): Flow<WorkResult<List<SettingItemData>>>
    fun history(): Flow<List<Pair<SettingItemData, UnixTimeMs>>>

    fun saveToHistory(item: SettingItem)
}

// The setting item data we expect the data layer to provide.
data class SettingItemData(
    // Unique id
    val id: String,
    val fieldName: String,
    val fieldValue: String,
)


