package se.kalind.searchanywhere.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput

/**
 * This composable is needed because combinedClickable does not pass the
 * click offset to the callbacks.
 */
@Composable
fun LongPressCard(
    modifier: Modifier = Modifier,
    onTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .clip(shape)
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        Log.d("LOGZ", "onTap")
                        onTap(offset)
                    },
                    onPress = { offset ->
                        Log.d("LOGZ", "onPress")
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        tryAwaitRelease()
                        interactionSource.emit(PressInteraction.Release(press))
                    },
                    onLongPress = { offset ->
                        Log.d("LOGZ", "onLongPress")
                        onLongPress(offset)
                    },
                )
            },
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}