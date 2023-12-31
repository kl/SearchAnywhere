package se.kalind.searchanywhere.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.ScanRoot
import javax.inject.Inject

typealias FileItems = List<WeightedItem<FileItem>>

class FilesUseCases @Inject constructor(
    private val filesRepository: FilesRepository,
) {
    val filteredFiles: Flow<WorkResult<FileItems>> =
        filesRepository.searchResults.map { searchResult ->
            searchResult.files.map { files ->
                val fileItems = files.map { FileItem(it) }
                filterItems(fileItems, searchResult.searchQuery)
            }
        }

    fun search(query: String) {
        filesRepository.setSearchQuery(query)
    }

    fun rebuildDatabase() {
        filesRepository.buildDatabase(ScanRoot.EXTERNAL_MAIN)
    }

    fun createDatabaseIfNeeded() {
        filesRepository.buildDatabaseIfNotExists(ScanRoot.EXTERNAL_MAIN)
    }
}
