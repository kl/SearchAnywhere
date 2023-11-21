package se.kalind.searchanywhere.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.repo.AppsRepository
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.SettingsRepository
import javax.inject.Inject

class HistoryUseCases @Inject constructor(
    private val appRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
    private val filesRepository: FilesRepository,
) {
    val getHistory: Flow<List<ItemType>> =
        combine(
            appRepository.history(),
            settingsRepository.history(),
            filesRepository.history()
        ) { appHist, settingHist, fileHist ->
            (appHist + settingHist + fileHist)
                .sortedByDescending { (_, updatedAt) -> updatedAt }
                .map { (item, _) -> item.toItemType() }
        }

    fun saveToHistory(item: ItemType) {
        when (item) {
            is ItemType.App -> appRepository.saveToHistory(item.item)
            is ItemType.Setting -> settingsRepository.saveToHistory(item.item)
            is ItemType.File -> filesRepository.saveToHistory(item.item)
        }
    }

    fun deleteFromHistory(item: ItemType) {
        when (item) {
            is ItemType.App -> appRepository.deleteFromHistory(item.item)
            is ItemType.Setting -> settingsRepository.deleteFromHistory(item.item)
            is ItemType.File -> filesRepository.deleteFromHistory(item.item)
        }
    }
}