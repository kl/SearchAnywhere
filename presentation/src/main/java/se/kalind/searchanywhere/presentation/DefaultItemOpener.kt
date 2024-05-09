package se.kalind.searchanywhere.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import se.kalind.searchanywhere.domain.ItemType
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.usecases.HistoryUseCase
import se.kalind.searchanywhere.domain.usecases.ItemOpener
import java.io.File

const val AUTHORITY = "se.kalind.searchanywhere.fileprovider"

class DefaultItemOpener(
    private val mainActivityRef: MainActivityReference,
    private val history: HistoryUseCase,
): ItemOpener {
    override fun openItem(item: ItemType) {
        // We should always have a current activity here
        val currentActivity = mainActivityRef.mainActivity ?: return

        when (item) {
            is ItemType.App -> {
                val app = item.item
                try {
                    val launchIntent =
                        currentActivity.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        currentActivity.startActivity(launchIntent)
                        history.saveToHistory(item)
                    } else {
                        Log.d("SearchAnywhere", "app unavailable")
//                        val ret = _messages.tryEmit(Message("Could not start app", app))
//                        Log.d("SearchAnywhere", "$ret")
                    }
                } catch (e: ActivityNotFoundException) {
                    Log.d("SearchAnywhere", "app unavailable")
//                    val ret = _messages.tryEmit(Message("Could not start app", app))
//                    Log.d("SearchAnywhere", "$ret")
                }
            }

            is ItemType.Setting -> {
                val setting = item.item
                try {
                    currentActivity.startActivity(Intent(setting.fieldValue))
                    history.saveToHistory(item)
                } catch (e: ActivityNotFoundException) {
                    Log.d("SearchAnywhere", "setting unavailable")
//                    val ret = _messages.tryEmit(Message("Setting unavailable", item))
//                    Log.d("SearchAnywhere", "$ret")
                }
            }

            is ItemType.File -> {
                history.saveToHistory(item)
                openFile(currentActivity, item.item)
            }
        }
    }

    private fun openFile(context: Context, fileItem: FileItem) {
        val file =
            Environment.getExternalStorageDirectory().absolutePath + "/${fileItem.displayName}"

        Log.d("SearchAnywhere", "open file: $file")
        val fileObject = File(file)

        val uri = FileProvider.getUriForFile(
            context, AUTHORITY, fileObject
        )
        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileObject.extension) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, type)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Define what your app should do if no activity can handle the intent.
            Log.d("SearchAnywhere", "failed to open file")
//            val ret = _messages.tryEmit(Message("Couldn't open file", file))
//            Log.d("SearchAnywhere", "$ret")
        }

    }
}