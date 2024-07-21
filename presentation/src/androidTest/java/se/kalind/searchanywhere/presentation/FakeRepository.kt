package se.kalind.searchanywhere.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FileSearchResult
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.MatchType
import se.kalind.searchanywhere.domain.repo.ScanRoot
import se.kalind.searchanywhere.domain.repo.SearchQuery
import javax.inject.Inject

class FakeRepository @Inject constructor() : FilesRepository {

    override val indexedFilesCount: Flow<Long> = flow { emit(2) }

    override val searchResults: Flow<FileSearchResult>
        get() = flowOf(
            FileSearchResult(
                searchQuery = listOf(
                    SearchQuery(
                        query = "file",
                        matchType = MatchType.INCLUDE,
                    )
                ),
                files = WorkResult.Success(arrayOf("file1", "file2")),
            )
        )

    override fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>> {
        return flowOf(emptyList())
    }

    override fun setSearchQuery(query: List<SearchQuery>) {}

    override suspend fun buildDatabase(scanRoot: ScanRoot) {}

    override suspend fun buildDatabaseIfNotExists(scanRoot: ScanRoot) {}

    override fun saveToHistory(item: FileItem) {}

    override fun deleteFromHistory(item: FileItem) {}
}
