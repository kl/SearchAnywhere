package se.kalind.searchanywhere.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import se.kalind.searchanywhere.ui.theme.AppTheme


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 5.dp,
                ) {
                    MainNavigation()
                }
            }
        }
    }

    private var requestFilePermissionCallback: ((Boolean) -> Unit)? = null
    private val requestFilePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d("LOGZ", permissions.toString())
            val allGranted = permissions.all { it.value }
            requestFilePermissionCallback?.invoke(allGranted)
            requestFilePermissionCallback = null
        }

    private var requestStorageManagerPermissionCallback: ((Boolean) -> Unit)? = null
    private var requestStorageManagerPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestStorageManagerPermissionCallback?.invoke(Environment.isExternalStorageManager())
            requestStorageManagerPermissionCallback = null
            if (!Environment.isExternalStorageManager()) {
                Log.d("LOGZ", "STORAGE MANAGER PERMISSION DENIED")
            }
        }
    }

    fun requestFilePermissions(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                callback(true)
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data =
                        Uri.parse(String.format("package:%s", packageName))
                    requestStorageManagerPermissionCallback = callback
                    requestStorageManagerPermission.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    requestStorageManagerPermissionCallback = callback
                    requestStorageManagerPermission.launch(intent)
                }
            }
        } else when {
            isGranted(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) -> {
                callback(true)
            }

            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                Log.d("LOGZ", "SHOULD SHOW")
            }

            else -> {
                requestFilePermissionCallback = callback
                requestFilePermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun isGranted(vararg permissions: String): Boolean {
        for (p in permissions) {
            val result =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}

fun Context.findMainActivity(): MainActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is MainActivity) return context
        context = context.baseContext
    }
    return null
}
