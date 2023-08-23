package se.kalind.searchanywhere.data.files

import android.content.Context
import android.os.Environment
import android.util.Log
import se.kalind.searchanywhere.domain.FilesRepository
import se.kalind.searchanywhere.domain.ScanRoot
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.FileItem
import java.io.File


class DefaultFilesRepository(
    private val context: Context,
    private val lib: AnlocateLibrary,
) : FilesRepository {

    val databaseFilePath =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/anlocate.txt"
    val externalMainScanRoot = Environment.getExternalStorageDirectory().absolutePath
    val tempDir =
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

    override fun saveToHistory(item: FileItem) {
    }
}
