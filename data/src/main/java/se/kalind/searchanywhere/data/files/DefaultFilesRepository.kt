package se.kalind.searchanywhere.data.files

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FileSearchResult
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.MatchType
import se.kalind.searchanywhere.domain.repo.ScanRoot
import se.kalind.searchanywhere.domain.repo.SearchQuery
import java.io.File
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class DefaultFilesRepository(
    private val lib: AnlocateLibrary,
    private val fileHistoryDao: FileHistoryDao,
    private val databaseFilePath: String,
    private val tempDirPath: String,
    private val scanDirRootPath: String,
    private val ioDispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope,
) : FilesRepository {

    private val _searchResults =
        MutableStateFlow(FileSearchResult(searchQuery = emptyList(), files = WorkResult.Loading))
    override val searchResults: Flow<FileSearchResult> = _searchResults

    private val _indexedFilesCount = MutableStateFlow(0L)
    override val indexedFilesCount: Flow<Long> = _indexedFilesCount

    override suspend fun buildDatabase(scanRoot: ScanRoot) {
        withContext(ioDispatcher) {
            try {
                val duration = measureTime {
                    lib.nativeBuildDatabase(databaseFilePath, scanDirRootPath, tempDirPath)
                }
                Log.i("SearchAnywhere", "native build db: ${duration.inWholeMilliseconds} ms")
                _indexedFilesCount.value = lib.nativeGetStatIndexedFiles(databaseFilePath)
            } catch (e: Exception) {
                if (e.message?.contains("permission denied", ignoreCase = true) == true) {
                    Log.e("SearchAnywhere", "buildDatabase: permission denied")
                } else {
                    throw e
                }
            }
        }
    }

    override suspend fun buildDatabaseIfNotExists(scanRoot: ScanRoot) {
        if (!File(databaseFilePath).isFile) {
            buildDatabase(scanRoot)
        }
    }

    override fun setSearchQuery(query: List<SearchQuery>) {
        appScope.launch(ioDispatcher) {
            try {
                if (File(databaseFilePath).isFile) {
                    val (q, includeExclude) = query.split()

                    val (files, duration) = measureTimedValue {
                        lib.nativeFindFiles(
                            dbFile = databaseFilePath,
                            query = q,
                            includeExclude = includeExclude,
                        )
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
            .flowOn(ioDispatcher)

    override fun saveToHistory(item: FileItem) {
        appScope.launch(ioDispatcher) {
            fileHistoryDao.saveToHistory(item.toEntity())
        }
    }

    override fun deleteFromHistory(item: FileItem) {
        appScope.launch(ioDispatcher) {
            fileHistoryDao.deleteFromHistory(item.toEntity())
        }
    }
}

// Convert a list of SearchQuery to the native library expected call format (two arrays)
private fun List<SearchQuery>.split(): Pair<Array<String>, BooleanArray> {
    val q = mutableListOf<String>()
    val i = mutableListOf<Boolean>()
    for (query in this) {
        q.add(query.query)
        i.add(query.matchType == MatchType.INCLUDE)
    }
    return q.toTypedArray() to i.toBooleanArray()
}

private fun FileItem.toEntity(): FileHistoryEntity {
    return FileHistoryEntity(
        fullPath = displayName,
        updateTime = System.currentTimeMillis(),
    )
}
