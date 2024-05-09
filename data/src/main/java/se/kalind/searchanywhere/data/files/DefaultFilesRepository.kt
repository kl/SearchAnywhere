package se.kalind.searchanywhere.data.files

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.ScanRoot
import se.kalind.searchanywhere.domain.repo.FileSearchResult
import java.io.File
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@OptIn(DelicateCoroutinesApi::class)
class DefaultFilesRepository(
    private val lib: AnlocateLibrary,
    private val fileHistoryDao: FileHistoryDao,
    private val databaseFilePath: String,
    private val tempDirPath: String,
    private val scanDirRootPath: String,
) : FilesRepository {

    private val _searchResults =
        MutableStateFlow(FileSearchResult(searchQuery = emptyList(), files = WorkResult.Loading))
    override val searchResults: Flow<FileSearchResult> = _searchResults

    override fun buildDatabase(scanRoot: ScanRoot) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val duration = measureTime {
                    lib.nativeBuildDatabase(databaseFilePath, scanDirRootPath, tempDirPath)
                }
                Log.i("SearchAnywhere","native build db: ${duration.inWholeMilliseconds} ms")
            } catch (e: Exception) {
                if (e.message?.contains("permission denied", ignoreCase = true) == true) {
                    Log.e("SearchAnywhere", "buildDatabase: permission denied")
                } else {
                    throw e
                }
            }
        }
    }

    override fun buildDatabaseIfNotExists(scanRoot: ScanRoot) {
        if (!File(databaseFilePath).isFile) {
            buildDatabase(scanRoot)
        }
    }

    override fun setSearchQuery(query: List<String>) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (File(databaseFilePath).isFile) {
                    val (files, duration) = measureTimedValue {
                        lib.nativeFindFiles(databaseFilePath, query.toTypedArray())
                    }
                    Log.i("SearchAnywhere", "native search: ${duration.inWholeMilliseconds} ms")
                    _searchResults.value = FileSearchResult(query, WorkResult.Success(files))
                } else {
                    // The database file may be missing because it hasn't finished building yet in which
                    // case we ignore that and return an empty result
                    Log.w("SearchAnywhere", "search: database file does not exist")
                    _searchResults.value = FileSearchResult(query, WorkResult.Success(emptyArray()))
                }
            } catch (e: Exception) {
                _searchResults.value = FileSearchResult(query, WorkResult.Error(e))
            }
        }
    }

    override fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>> =
        fileHistoryDao.getLatestHistory().map { history ->
            history.map { hist ->
                val item = FileItem(hist.fullPath)
                Pair(item, hist.updateTime)
            }
        }
            .flowOn(Dispatchers.IO)

    override fun saveToHistory(item: FileItem) {
        GlobalScope.launch(Dispatchers.IO) {
            fileHistoryDao.saveToHistory(item.toEntity())
        }
    }

    override fun deleteFromHistory(item: FileItem) {
        GlobalScope.launch(Dispatchers.IO) {
            fileHistoryDao.deleteFromHistory(item.toEntity())
        }
    }
}

private fun FileItem.toEntity(): FileHistoryEntity {
    return FileHistoryEntity(
        fullPath = displayName,
        updateTime = System.currentTimeMillis(),
    )
}
