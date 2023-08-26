package se.kalind.searchanywhere.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.ScanRoot
import javax.inject.Inject

typealias FileItems = List<WeightedItem<FileItem>>

class FilesUseCases @Inject constructor(
    private val filesRepository: FilesRepository,
) {
    private val _files: MutableStateFlow<WorkResult<FileItems>> =
        MutableStateFlow(WorkResult.Loading)
    val filteredFiles: Flow<WorkResult<FileItems>> = _files

    fun rebuildDatabase() {
        filesRepository.buildDatabase(ScanRoot.EXTERNAL_MAIN)
    }

    fun createDatabaseIfNeeded() {
        filesRepository.buildDatabaseIfNotExists(ScanRoot.EXTERNAL_MAIN)
    }

    fun setFilter(filter: String) {
        val searchResult = filesRepository.search(filter)
        val filtered = searchResult.map { files ->
            val fileItems = files.map { FileItem(it) }
            filterItems(fileItems, filter)
        }
        _files.value = filtered
    }
}
