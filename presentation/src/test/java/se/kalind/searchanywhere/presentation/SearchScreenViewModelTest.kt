@file:OptIn(ExperimentalCoroutinesApi::class)

package se.kalind.searchanywhere.presentation

import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.AppIconDrawable
import se.kalind.searchanywhere.domain.repo.AppItemData
import se.kalind.searchanywhere.domain.repo.FileSearchResult
import se.kalind.searchanywhere.domain.repo.MatchType
import se.kalind.searchanywhere.domain.repo.SearchQuery
import se.kalind.searchanywhere.domain.repo.SettingItemData
import se.kalind.searchanywhere.domain.usecases.AppsUseCase
import se.kalind.searchanywhere.domain.usecases.FilesUseCase
import se.kalind.searchanywhere.domain.usecases.HistoryUseCase
import se.kalind.searchanywhere.domain.usecases.OpenItemUseCase
import se.kalind.searchanywhere.domain.usecases.SettingsUseCase
import se.kalind.searchanywhere.presentation.search.SearchScreenViewModel
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

@RunWith(MockitoJUnitRunner::class)
class SearchScreenViewModelTest {

    lateinit var appsRepo: FakeAppsRepo
    lateinit var settingsRepo: FakeSettingsRepo
    lateinit var filesRepo: FakeFilesRepo
    lateinit var ucOpen: OpenItemUseCase
    lateinit var ucApps: AppsUseCase
    lateinit var ucSettings: SettingsUseCase
    lateinit var ucFiles: FilesUseCase
    lateinit var ucHistory: HistoryUseCase

    @Before
    fun setup() {
        appsRepo = FakeAppsRepo()
        settingsRepo = FakeSettingsRepo()
        filesRepo = FakeFilesRepo()
        ucOpen = OpenItemUseCase(FakeItemOpener())
        ucApps = AppsUseCase(appsRepo)
        ucSettings = SettingsUseCase(settingsRepo)
        ucFiles = FilesUseCase(filesRepo)
        ucHistory = HistoryUseCase(appsRepo, settingsRepo, filesRepo)
    }

    fun createViewModel(dispatcher: TestDispatcher): SearchScreenViewModel =
        SearchScreenViewModel(
            ucOpen = ucOpen,
            ucApps = ucApps,
            ucSettings = ucSettings,
            ucFiles = ucFiles,
            ucHistory = ucHistory,
            defaultDispatcher = dispatcher,
        )

    @Test
    fun uiState_emptyState() = runTest {
        // test empty state
        val viewModel = createViewModel(dispatcher)
        viewModel.uiState.take(1).collect() { state ->
            assertTrue(state.items.isEmpty())
            assertEquals(Loading(null), state.history)
        }
    }

    @Test
    fun uiState_combined() = runTestSetMain {
        // test that ui state is created correctly when there is data

        filesRepo.filesFlow.value = FileSearchResult(
            searchQuery = listOf(
                SearchQuery(
                    query = "file",
                    matchType = MatchType.INCLUDE
                )
            ),
            files = WorkResult.Success(arrayOf("file1", "file2.mp3"))
        )
        appsRepo.appsFlow.value = WorkResult.Success(
            listOf(
                AppItemData(
                    id = "some.id",
                    label = "X The Everything Files App",
                    packageName = "com.the.package",
                    activityName = "activity",
                    icon = AppIconDrawable(mock<Drawable>())
                )
            )
        )
        settingsRepo.settingsFlow.value = WorkResult.Success(
            listOf(
                SettingItemData(
                    id = "settingId",
                    fieldName = "settings_files",
                    fieldValue = "some_val",
                ),
                SettingItemData(
                    id = "wifiId",
                    fieldName = "settings_wifi",
                    fieldValue = "some_val",
                ),
            )
        )

        val viewModel = createViewModel(dispatcher)
        ucApps.setFilter("file")
        ucSettings.setFilter("file")
        ucFiles.search("file")

        val state = viewModel.uiState.take(2).last()

        assertTrue("items should not be empty", state.items.isNotEmpty())
        assertFalse("history should have finished loading", state.history.isLoading())

        assertNotNull("app name should be in items list",
            state.items.find { it.displayName.contains("X The Everything Files App") })

        assertNotNull("file name should be in items list",
            state.items.find { it.displayName.contains("file2.mp3") })

        assertNotNull("settings Files should be in items list",
            state.items.find { it.displayName.contains("Files") })

        assertNull("settings Wifi should not be in the list",
            state.items.find { it.displayName.contains("Wifi") })
    }
}

fun runTestSetMain(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration? = null,
    testBody: suspend TestScope.() -> Unit
): TestResult {
    try {
        return if (timeout != null) {
            runTest(context, timeout) {
                Dispatchers.setMain(dispatcher)
                testBody()
            }
        } else {
            runTest(context) {
                Dispatchers.setMain(dispatcher)
                testBody()
            }
        }
    } finally {
        Dispatchers.resetMain()
    }
}

val TestScope.dispatcher: TestDispatcher get() = coroutineContext[ContinuationInterceptor] as TestDispatcher
