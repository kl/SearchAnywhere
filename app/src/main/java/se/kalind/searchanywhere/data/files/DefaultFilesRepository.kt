package se.kalind.searchanywhere.data.files

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.ScanRoot
import java.io.File

class DefaultFilesRepository(
    private val lib: AnlocateLibrary,
    private val fileHistoryDao: FileHistoryDao,
) : FilesRepository {

    private val databaseFilePath =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/anlocate.txt"
    private val externalMainScanRoot: String =
        Environment.getExternalStorageDirectory().absolutePath
    private val tempDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/anlocate_temp"

    override fun buildDatabase(scanRoot: ScanRoot) {
        try {
            lib.nativeBuildDatabase(databaseFilePath, externalMainScanRoot, tempDir)
        } catch (e: Exception) {
            if (e.message?.contains("permission denied", ignoreCase = true) == true) {
                Log.e("LOGZ", "buildDatabase: permission denied")
            } else {
                throw e
            }
        }
    }

    override fun buildDatabaseIfNotExists(scanRoot: ScanRoot) {
        if (!File(databaseFilePath).isFile) {
            buildDatabase(scanRoot)
        }
    }

    override fun search(query: String): WorkResult<Array<String>> {
        return try {
            WorkResult.Success(lib.nativeFindFiles(databaseFilePath, query))
        } catch (e: Exception) {
            WorkResult.Error(e)
        }
    }

    override fun history(): Flow<List<Pair<FileItem, UnixTimeMs>>> {
        return fileHistoryDao.getLatestHistory().map { history ->
            history.map { hist ->
                val item = FileItem(hist.fullPath)
                Pair(item, hist.updateTime)
            }
        }
    }

    override fun saveToHistory(item: FileItem) {
        val entity = FileHistoryEntity(
            fullPath = item.displayName,
            updateTime = System.currentTimeMillis(),
        )
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            fileHistoryDao.saveToHistory(entity)
        }
    }
}
