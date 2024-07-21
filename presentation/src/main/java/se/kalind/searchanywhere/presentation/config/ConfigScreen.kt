package se.kalind.searchanywhere.presentation.config

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.kalind.searchanywhere.presentation.R

@Composable
fun SettingsScreen(
    viewModel: ConfigViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.padding(top = 16.dp)) {
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
            style = MaterialTheme.typography.labelMedium
        )
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
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                HintsButton()
            }
        }
    }
}

@Composable
fun HintsButton() {
    var showHints by remember { mutableStateOf(false) }
    TextButton(onClick = { showHints = true }) {
        Text("Show Hints")
    }
    if (showHints) {
        AlertDialog(
            onDismissRequest = { showHints = false },
            confirmButton = {
                TextButton(onClick = { showHints = false }) {
                    Text("Close")
                }
            },
            text = {
                Column {
                    Text("Advanced Search\n", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = AnnotatedString.fromHtml(
                            htmlString = stringResource(id = R.string.search_hints)
                        )
                    )
                }
            }
        )
    }
}

