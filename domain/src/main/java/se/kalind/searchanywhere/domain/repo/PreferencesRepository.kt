package se.kalind.searchanywhere.domain.repo

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    val reindexOnStartup: Flow<Boolean>
    fun setReindexOnStartup(reindex: Boolean)
}
