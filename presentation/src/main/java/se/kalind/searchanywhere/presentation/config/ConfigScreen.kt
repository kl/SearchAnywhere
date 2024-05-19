package se.kalind.searchanywhere.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    insetsPadding: PaddingValues
) {
    Box(modifier = Modifier.padding(insetsPadding)) {
        SettingsContent()
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsContent() {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
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
                modifier = Modifier.padding(16.dp)
            ) {
                Button(onClick = {}) {
                    Text("Re-index files")
                }
                Text(modifier = Modifier.padding(top = 5.dp), text = "Indexed files: 12643")
                Spacer(modifier = Modifier.height(12.dp))
                Switch(checked = true, onCheckedChange = {})
                Text("Re-index on startup")
            }
        }
    }
}

