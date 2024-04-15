package se.kalind.searchanywhere.domain.usecases

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.SettingItem
import se.kalind.searchanywhere.domain.repo.SettingsRepository
import javax.inject.Inject

typealias SettingItems = Sequence<WeightedItem<SettingItem>>

class GetSettingsUseCase @Inject constructor(settingsRepository: SettingsRepository) {

    private val _filter: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    val filteredSettings: Flow<WorkResult<SettingItems>> =
        settingsRepository.availableSettings().combine(_filter) { settings, filter ->
            settings.map { settingsListData ->
                val settingItems = settingsListData.asSequence().map(SettingItem::fromData)
                filterItems(settingItems, filter)
            }
        }

    fun setFilter(filter: String) {
        _filter.value = splitFilter(filter)
    }
}
