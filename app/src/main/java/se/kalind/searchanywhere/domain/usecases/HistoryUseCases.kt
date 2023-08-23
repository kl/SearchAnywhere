package se.kalind.searchanywhere.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import se.kalind.searchanywhere.domain.AppsRepository
import se.kalind.searchanywhere.domain.FilesRepository
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.SettingsRepository
import javax.inject.Inject

class HistoryUseCases @Inject constructor(
    private val appRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
    private val filesRepository: FilesRepository,
) {
    val getHistory: Flow<List<ItemType>> =
        combine(appRepository.history(), settingsRepository.history()) { appHist, settingHist ->
            val appItems = appHist.map { item ->
                val (data, updatedAt) = item
                val itemType = ItemType.App(AppItem.fromData(data))
                Pair(itemType, updatedAt)
            }
            val settingItems = settingHist.map { item ->
                val (data, updatedAt) = item
                val itemType = ItemType.Setting(SettingItem.fromData(data))
                Pair(itemType, updatedAt)
            }
            (appItems + settingItems)
                .sortedByDescending { (_, updatedAt) -> updatedAt }
                .map { (item, _) -> item }
        }

    fun saveToHistory(item: ItemType) {
        when (item) {
            is ItemType.App -> appRepository.saveToHistory(item.item)
            is ItemType.Setting -> settingsRepository.saveToHistory(item.item)
            is ItemType.File -> filesRepository.saveToHistory(item.item)
        }
    }
}