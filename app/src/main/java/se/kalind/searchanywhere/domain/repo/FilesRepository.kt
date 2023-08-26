package se.kalind.searchanywhere.domain.repo

import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.ToItemType
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.DisplayName

interface FilesRepository {
    fun buildDatabase(scanRoot: ScanRoot)
    fun buildDatabaseIfNotExists(scanRoot: ScanRoot)
    fun search(query: String): WorkResult<Array<String>>
    fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>>
    fun saveToHistory(item: FileItem)
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
