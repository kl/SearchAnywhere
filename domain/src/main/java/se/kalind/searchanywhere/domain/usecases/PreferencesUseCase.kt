package se.kalind.searchanywhere.domain.usecases

import kotlinx.coroutines.flow.Flow
import se.kalind.searchanywhere.domain.repo.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    val reindexOnStartup: Flow<Boolean> = preferencesRepository.reindexOnStartup

    fun setReindexOnStartup(reindex: Boolean) {
        preferencesRepository.setReindexOnStartup(reindex)
    }
}