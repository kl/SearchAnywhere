package se.kalind.searchanywhere.presentation.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.usecases.FilesUseCase
import se.kalind.searchanywhere.domain.usecases.PreferencesUseCase
import javax.inject.Inject

data class UiState(
    val indexedFilesCount: Long = 0,
    val reindexOnStartup: Boolean = false,
    val reindexButtonEnabled: Boolean = true
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val ucFiles: FilesUseCase,
    private val ucPrefs: PreferencesUseCase,
) : ViewModel() {

    // Needed to prevent yank when toggling the switch
    private val _reindexOnStartup = MutableStateFlow(false)

    private val _reindexButtonEnabled = MutableStateFlow(true)

    val uiState: StateFlow<UiState> = combine(
        ucFiles.indexedFilesCount,
        merge(_reindexOnStartup, ucPrefs.reindexOnStartup),
        _reindexButtonEnabled,
    ) { indexedFileCount, reindexOnStartup, reindexButtonEnabled ->
        UiState(
            indexedFilesCount = indexedFileCount,
            reindexOnStartup = reindexOnStartup,
            reindexButtonEnabled = reindexButtonEnabled
        )
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun reindexOnStartup(reindex: Boolean) {
        _reindexOnStartup.value = reindex
        ucPrefs.setReindexOnStartup(reindex)
    }

    fun onReindexClick() {
        _reindexButtonEnabled.value = false
        viewModelScope.launch {
            ucFiles.rebuildDatabase()
            _reindexButtonEnabled.value = true
        }
    }
}
