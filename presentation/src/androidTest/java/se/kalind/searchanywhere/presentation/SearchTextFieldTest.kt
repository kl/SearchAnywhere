package se.kalind.searchanywhere.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import se.kalind.searchanywhere.presentation.appbar.SearchTextField

class SearchTextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun iconsAreDisplayed() {
        composeTestRule.setContent {
            var query by remember { mutableStateOf("query") }
            SearchTextField(
                text = query,
                onSearchChanged = {
                    query = it
                }) {
            }
        }

        // Check initial search field value
        composeTestRule.onNodeWithText("query").assertExists()

        // Click the clear text icon
        val clearIcon = composeTestRule.onNodeWithContentDescription("clear text")
        clearIcon.assertExists()
        clearIcon.performClick()

        // Check search field is empty
        composeTestRule.onNodeWithText("query").assertDoesNotExist()
        composeTestRule.onNodeWithText("").assertExists()

        // Check that icons changed
        composeTestRule.onNodeWithContentDescription("clear text").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("search icon").assertExists()

//      composeTestRule.onRoot(useUnmergedTree = true).printToLog("currentLabelExists")
    }
}