package se.kalind.searchanywhere.presentation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.usecases.FilesUseCase
import se.kalind.searchanywhere.domain.usecases.PreferencesUseCase
import se.kalind.searchanywhere.presentation.theme.AppTheme
import javax.inject.Inject

interface PermissionStatusCallback {
    fun onGranted()
    fun onDenied()
    fun onShowRationale(afterShown: () -> Unit)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var files: FilesUseCase

    @Inject
    lateinit var prefs: PreferencesUseCase

    @Inject
    lateinit var appScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkRebuildDb()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier
                        // Make sure bottom app nav bar doesn't overlap the system navigation bar.
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        // When the soft keyboard is shown add the keyboard padding. This will
                        // animate the bottom app nav bar up over the keyboard.
                        .windowInsetsPadding(WindowInsets.ime)
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 5.dp,
                ) {
                    App()
                }
            }
        }
    }

    private fun checkRebuildDb() {
        appScope.launch {
            if (isFilePermissionsGranted() && prefs.reindexOnStartup.first()) {
                files.rebuildDatabase()
            }
        }
    }

    private var requestFilePermissionCallback: PermissionStatusCallback? = null
    private val filePermissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                requestFilePermissionCallback?.onGranted()
            } else {
                requestFilePermissionCallback?.onDenied()
            }
            requestFilePermissionCallback = null
        }

    private var requestStorageManagerPermissionCallback: PermissionStatusCallback? = null
    private var storageManagerPermissionRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                requestStorageManagerPermissionCallback?.onGranted()
            } else {
                requestStorageManagerPermissionCallback?.onDenied()
            }
            requestStorageManagerPermissionCallback = null
        }
    }

    fun requestFilePermissions(callback: PermissionStatusCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (isFilePermissionsGranted()) {
                callback.onGranted()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    )
                ) {
                    callback.onShowRationale {
                        requestStorageManagerPermission(callback)
                    }
                } else {
                    requestStorageManagerPermission(callback)
                }
            }
        } else when {
            isFilePermissionsGranted() -> {
                callback.onGranted()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                callback.onShowRationale {
                    requestReadWriteFilePermissions(callback)
                }
            }

            else -> {
                requestReadWriteFilePermissions(callback)
            }
        }
    }

    private fun requestReadWriteFilePermissions(callback: PermissionStatusCallback) {
        requestFilePermissionCallback = callback
        filePermissionRequest.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestStorageManagerPermission(callback: PermissionStatusCallback) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data =
                Uri.parse(String.format("package:%s", packageName))
            requestStorageManagerPermissionCallback = callback
            storageManagerPermissionRequest.launch(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            requestStorageManagerPermissionCallback = callback
            storageManagerPermissionRequest.launch(intent)
        }
    }

    private fun isFilePermissionsGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            isGranted(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }

    private fun isGranted(vararg permissions: String): Boolean {
        for (p in permissions) {
            val result =
                ContextCompat.checkSelfPermission(this, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
