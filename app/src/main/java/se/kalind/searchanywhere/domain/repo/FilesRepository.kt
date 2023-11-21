package se.kalind.searchanywhere.domain.repo

import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.ToItemType
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.DisplayName

interface FilesRepository {

    val searchResults: Flow<FileSearchResult>

    fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>>

    fun setSearchQuery(query: String)
    fun buildDatabase(scanRoot: ScanRoot)
    fun buildDatabaseIfNotExists(scanRoot: ScanRoot)
    fun saveToHistory(item: FileItem)
    fun deleteFromHistory(item: FileItem)
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
    val searchQuery: String,
    val files: WorkResult<Array<String>>
)
