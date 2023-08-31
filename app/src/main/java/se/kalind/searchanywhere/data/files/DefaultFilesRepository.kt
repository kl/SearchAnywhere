package se.kalind.searchanywhere.data.files

import android.os.Environment
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
import kotlin.time.measureTimedValue

@OptIn(DelicateCoroutinesApi::class)
class DefaultFilesRepository(
    private val lib: AnlocateLibrary,
    private val fileHistoryDao: FileHistoryDao,
) : FilesRepository {

    private val _searchResults =
        MutableStateFlow(FileSearchResult(searchQuery = "", files = WorkResult.Loading))
    override val searchResults: Flow<FileSearchResult> = _searchResults

    private val databaseFilePath =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/anlocate.txt"
    private val externalMainScanRoot: String =
        Environment.getExternalStorageDirectory().absolutePath
    private val tempDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/anlocate_temp"

    override fun buildDatabase(scanRoot: ScanRoot) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                lib.nativeBuildDatabase(databaseFilePath, externalMainScanRoot, tempDir)
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
        GlobalScope.launch(Dispatchers.IO) {
            if (!File(databaseFilePath).isFile) {
                buildDatabase(scanRoot)
            }
        }
    }

    override fun setSearchQuery(query: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (File(databaseFilePath).isFile) {
                    val (files, duration) = measureTimedValue {
                        lib.nativeFindFiles(databaseFilePath, query)
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
        val entity = FileHistoryEntity(
            fullPath = item.displayName,
            updateTime = System.currentTimeMillis(),
        )
        GlobalScope.launch(Dispatchers.IO) {
            fileHistoryDao.saveToHistory(entity)
        }
    }
}
