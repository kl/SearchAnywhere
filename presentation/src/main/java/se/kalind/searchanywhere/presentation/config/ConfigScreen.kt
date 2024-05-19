package se.kalind.searchanywhere.presentation.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: ConfigViewModel = hiltViewModel(),
    insetsPadding: PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.padding(insetsPadding)) {
        SettingsContent(
            indexedFiles = state.indexedFilesCount,
            reindexOnStartup = state.reindexOnStartup,
            reindexButtonEnabled = state.reindexButtonEnabled,
            onReindexClick = viewModel::onReindexClick,
            onReindexOnStartupChanged = viewModel::reindexOnStartup
        )
    }
}

@Composable
fun SettingsContent(
    indexedFiles: Long,
    reindexOnStartup: Boolean,
    reindexButtonEnabled: Boolean,
    onReindexClick: () -> Unit,
    onReindexOnStartupChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 32.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 3.dp),
            text = "Settings",
            style = MaterialTheme.typography.labelMedium)
        Surface(
            tonalElevation = 5.dp,
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))

        ) {
            Column(
                modifier = Modifier.padding(16.dp),

            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        enabled = reindexButtonEnabled,
                        onClick = onReindexClick
                    ) {
                        Text("Re-index files")
                    }
                    if (!reindexButtonEnabled) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .requiredSize(32.dp)
                                .padding(start = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 5.dp),
                    text = if (indexedFiles > 0) "Indexed files: $indexedFiles" else "",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Switch(
                    checked = reindexOnStartup,
                    onCheckedChange = onReindexOnStartupChanged
                )
                Text("Re-index on startup")
            }
        }
    }
}

