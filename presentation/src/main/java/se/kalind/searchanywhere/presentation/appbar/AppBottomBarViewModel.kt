package se.kalind.searchanywhere.presentation.appbar

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.usecases.AppsUseCase
import se.kalind.searchanywhere.domain.usecases.FilesUseCase
import se.kalind.searchanywhere.domain.usecases.SettingsUseCase
import se.kalind.searchanywhere.presentation.MainActivityReference
import se.kalind.searchanywhere.presentation.PermissionStatusCallback
import javax.inject.Inject

@HiltViewModel
class AppBottomBarViewModel @Inject constructor(
    private val mainActivityRef: MainActivityReference,
    private val ucSettings: SettingsUseCase,
    private val ucApps: AppsUseCase,
    private val ucFiles: FilesUseCase,
) : ViewModel() {

    // Keeps track of whether we are allowed to read/write files on external storage.
    // If this is false we do not try to search the file database.
    private var filePermissionsGranted = false

    private val _currentSearchText = MutableStateFlow("")
    val currentSearchText = _currentSearchText.asStateFlow()

    // When set to a non-null value a dialog is shown explaining why we ask for file permissions.
    private val _showPermissionRationale: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)
    val showPermissionRationale: StateFlow<(() -> Unit)?> = _showPermissionRationale

    fun onSearchChanged(text: String) {
        _currentSearchText.value = text
        ucSettings.setFilter(text)
        ucApps.setFilter(text)
        if (filePermissionsGranted) {
            ucFiles.search(text)
        }
    }

    fun onSearchFieldFocused(isFocused: Boolean) {
        if (isFocused) {
            handlePermissionsRequest()
        }
    }

    private fun handlePermissionsRequest() {
        val activity = mainActivityRef.mainActivity ?: return

        activity.requestFilePermissions(object : PermissionStatusCallback {
            override fun onGranted() {
                Log.d("SearchAnywhere", "file permissions granted")
                viewModelScope.launch {
                    ucFiles.createDatabaseIfNeeded()
                }
                filePermissionsGranted = true
            }

            override fun onDenied() {
                Log.d("SearchAnywhere", "file permissions denied")
                filePermissionsGranted = false
            }

            override fun onShowRationale(afterShown: () -> Unit) {
                // The callback in _showPermissionRationale is called from Compose after the user
                // has clicked away the rationale dialog. afterShown() will then trigger another
                // permissions request which will call onGranted() or onDenied() on this object.
                _showPermissionRationale.value = {
                    afterShown()
                    _showPermissionRationale.value = null
                }
            }
        })
    }
}
