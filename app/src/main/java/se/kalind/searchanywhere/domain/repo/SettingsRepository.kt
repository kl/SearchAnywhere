package se.kalind.searchanywhere.domain.repo

import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.ToItemType
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.DisplayName

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
) : ToItemType {
    override fun toItemType(): ItemType {
        return SettingItem.fromData(this).toItemType()
    }
}

// The domain layer setting item representation.
data class SettingItem(
    val id: String,
    val fieldName: String,
    val fieldValue: String,
    override val displayName: String,
) : DisplayName, ToItemType {
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

    override fun toItemType(): ItemType {
        return ItemType.Setting(this)
    }
}
