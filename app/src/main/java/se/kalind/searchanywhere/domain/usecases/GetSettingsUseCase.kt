package se.kalind.searchanywhere.domain.usecases

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import se.kalind.searchanywhere.domain.SettingItemData
import se.kalind.searchanywhere.domain.SettingsRepository
import se.kalind.searchanywhere.domain.WorkResult
import javax.inject.Inject

// The domain layer setting item representation.
data class SettingItem(
    val id: String,
    val fieldName: String,
    val fieldValue: String,
    override val displayName: String,
) : DisplayName {
    companion object {
        fun fromData(settings: SettingItemData): SettingItem {
            return SettingItem(
                id = settings.id,
                fieldName = settings.fieldName,
                fieldValue = settings.fieldValue,
                displayName = settings.fieldName.lowercase()
                    .split("_")
                    .drop(1)
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { it.uppercase() }
                    })
        }
    }
}


typealias SettingItems = List<WeightedItem<SettingItem>>

class GetSettingsUseCase @Inject constructor(settingsRepository: SettingsRepository) {

    private val _filter = MutableStateFlow("")

    val filteredSettings: Flow<WorkResult<SettingItems>> =
        settingsRepository.availableSettings().combine(_filter) { settings, filter ->
            val filtered = settings.map { settingsListData ->
                val settingItems = settingsListData.map(SettingItem::fromData)
                filterItems(settingItems, filter)
            }
            Log.d("LOGZ", "emit filtered settings")
            filtered
        }

    fun setFilter(filter: String) {
        _filter.value = filter
    }
}

