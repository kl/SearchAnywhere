package se.kalind.searchanywhere.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.ScanRoot
import javax.inject.Inject
import javax.inject.Singleton

typealias FileItems = Sequence<WeightedItem<FileItem>>

@Singleton
class FilesUseCase @Inject constructor(
    private val filesRepository: FilesRepository,
) {
    val filteredFiles: Flow<WorkResult<FileItems>> =
        filesRepository.searchResults.map { searchResult ->
            searchResult.files.map { files ->
                val fileItems = files.asSequence().map { FileItem(it) }
                weighFiles(fileItems, searchResult.searchQuery)
            }
        }

    val indexedFilesCount: Flow<Long> = filesRepository.indexedFilesCount

    fun search(query: String) {
        filesRepository.setSearchQuery(splitFilter(query))
    }

    suspend fun rebuildDatabase() {
        filesRepository.buildDatabase(ScanRoot.EXTERNAL_MAIN)
    }

    suspend fun createDatabaseIfNeeded() {
        filesRepository.buildDatabaseIfNotExists(ScanRoot.EXTERNAL_MAIN)
    }
}
