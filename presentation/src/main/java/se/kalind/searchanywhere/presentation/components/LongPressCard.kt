package se.kalind.searchanywhere.presentation.components

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This composable is needed because combinedClickable does not pass the
 * click offset to the callbacks.
 */
@Composable
fun LongPressCard(
    modifier: Modifier = Modifier,
    onTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    listState: LazyListState,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable() (ColumnScope.() -> Unit),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cs = rememberCoroutineScope();

    Card(
        modifier = modifier
            .clip(shape)
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        onTap(offset)
                    },
                    onPress = { offset ->
                        cs.launch {
                            // Delay is needed to not show ripple when initiating the scroll
                            delay(100)
                            if (!listState.isScrollInProgress) {
                                val press = PressInteraction.Press(offset)
                                interactionSource.emit(press)
                                tryAwaitRelease()
                                interactionSource.emit(PressInteraction.Release(press))
                            }
                        }
                    },
                    onLongPress = { offset ->
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