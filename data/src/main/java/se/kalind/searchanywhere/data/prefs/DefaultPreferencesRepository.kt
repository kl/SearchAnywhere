package se.kalind.searchanywhere.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.repo.PreferencesRepository

private val keyReindexOnStartup = booleanPreferencesKey("reindex_on_startup")
private const val keyReindexOnStartupDefault = true

class DefaultPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope,
) : PreferencesRepository {

    override val reindexOnStartup: Flow<Boolean>
        get() = dataStore.data.map { prefs ->
            prefs[keyReindexOnStartup] ?: keyReindexOnStartupDefault
        }

    override fun setReindexOnStartup(reindex: Boolean) {
        appScope.launch(ioDispatcher) {
            dataStore.edit { prefs ->
                prefs[keyReindexOnStartup] = reindex
            }
        }
    }
}