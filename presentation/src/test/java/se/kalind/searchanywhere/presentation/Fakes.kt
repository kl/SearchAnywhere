package se.kalind.searchanywhere.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.ok
import se.kalind.searchanywhere.domain.repo.AppItem
import se.kalind.searchanywhere.domain.repo.AppItemData
import se.kalind.searchanywhere.domain.repo.AppsRepository
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FileSearchResult
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.ScanRoot
import se.kalind.searchanywhere.domain.repo.SearchQuery
import se.kalind.searchanywhere.domain.repo.SettingItem
import se.kalind.searchanywhere.domain.repo.SettingItemData
import se.kalind.searchanywhere.domain.repo.SettingsRepository
import se.kalind.searchanywhere.domain.usecases.ItemOpener

class FakeAppsRepo : AppsRepository {

    val appsFlow = MutableStateFlow<WorkResult<List<AppItemData>>>(WorkResult.Success(emptyList()))

    val historyFlow = MutableStateFlow<List<Pair<AppItemData, UnixTimeMs>>>(emptyList())

    override fun availableApps(): Flow<WorkResult<List<AppItemData>>> {
        return appsFlow
    }

    override fun history(): Flow<List<Pair<AppItemData, UnixTimeMs>>> {
        return historyFlow
    }

    override fun saveToHistory(item: AppItem) {}

    override fun deleteFromHistory(item: AppItem) {}
}

class FakeSettingsRepo : SettingsRepository {

    val settingsFlow = MutableStateFlow<WorkResult<List<SettingItemData>>>(WorkResult.Success(
        emptyList()
    ))

    val historyFlow = MutableStateFlow<List<Pair<SettingItemData, UnixTimeMs>>>(emptyList())

    override fun availableSettings(): Flow<WorkResult<List<SettingItemData>>> {
        return settingsFlow
    }

    override fun history(): Flow<List<Pair<SettingItemData, UnixTimeMs>>> {
        return historyFlow
    }

    override fun saveToHistory(item: SettingItem) {}

    override fun deleteFromHistory(item: SettingItem) {}
}

class FakeFilesRepo() : FilesRepository {

    val filesFlow = MutableStateFlow(FileSearchResult(
        searchQuery = emptyList(),
        files = WorkResult.Success(emptyArray()))
    )

    val historyFlow = MutableStateFlow<List<Pair<FileItem, UnixTimeMs>>>(emptyList())

    override val searchResults: Flow<FileSearchResult>
        get() = filesFlow

    override val indexedFilesCount: Flow<Long>
        get() = flow { emit(0) }

    override fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>> {
        return historyFlow
    }

    override fun setSearchQuery(query: List<SearchQuery>) {}

    override fun saveToHistory(item: FileItem) {}

    override fun deleteFromHistory(item: FileItem) {}

    override suspend fun buildDatabase(scanRoot: ScanRoot) {}

    override suspend fun buildDatabaseIfNotExists(scanRoot: ScanRoot) {}
}

class FakeItemOpener : ItemOpener {
    override fun openItem(item: ItemType): Result<Unit> {
        return Result.ok()
    }
}
