package se.kalind.searchanywhere.domain.repo

import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.ToItemType
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.DisplayName

interface FilesRepository {

    val searchResults: Flow<FileSearchResult>
    val indexedFilesCount: Flow<Long>
    fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>>

    fun setSearchQuery(query: List<SearchQuery>)
    suspend fun buildDatabase(scanRoot: ScanRoot)
    suspend fun buildDatabaseIfNotExists(scanRoot: ScanRoot)
    fun saveToHistory(item: FileItem)
    fun deleteFromHistory(item: FileItem)
}

data class SearchQuery(
    val query: String,
    val matchType: MatchType
)

enum class MatchType {
    INCLUDE,
    EXCLUDE,
}

enum class ScanRoot {
    EXTERNAL_MAIN,
    EXTERNAL_ALL,
}

data class FileItem(override val displayName: String) : DisplayName, ToItemType {
    override fun toItemType(): ItemType {
        return ItemType.File(this)
    }
}

data class FileSearchResult(
    val searchQuery: List<SearchQuery>,
    val files: WorkResult<Array<String>>
)
