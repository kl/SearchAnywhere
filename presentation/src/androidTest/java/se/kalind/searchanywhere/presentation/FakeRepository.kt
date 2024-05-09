package se.kalind.searchanywhere.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FileSearchResult
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.ScanRoot
import javax.inject.Inject

class FakeRepository @Inject constructor() : FilesRepository {
    override val searchResults: Flow<FileSearchResult>
        get() = flowOf(
            FileSearchResult(
            searchQuery = listOf("file"),
            files = WorkResult.Success(arrayOf("file1", "file2"))
        )
        )

    override fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>> {
        return flowOf(emptyList())
    }

    override fun setSearchQuery(query: List<String>) {}

    override fun buildDatabase(scanRoot: ScanRoot) {}

    override fun buildDatabaseIfNotExists(scanRoot: ScanRoot) {}

    override fun saveToHistory(item: FileItem) {}

    override fun deleteFromHistory(item: FileItem) {}
}
