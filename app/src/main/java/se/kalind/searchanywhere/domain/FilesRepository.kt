package se.kalind.searchanywhere.domain

import se.kalind.searchanywhere.domain.usecases.FileItem

interface FilesRepository {
    fun buildDatabase(scanRoot: ScanRoot)
    fun buildDatabaseIfNotExists(scanRoot: ScanRoot)
    fun search(query: String): WorkResult<Array<String>>
    fun saveToHistory(item: FileItem)
}

enum class ScanRoot {
    EXTERNAL_MAIN,
    EXTERNAL_ALL,
}
