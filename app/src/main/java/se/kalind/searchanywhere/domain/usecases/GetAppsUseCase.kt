package se.kalind.searchanywhere.domain.usecases

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.AppItem
import se.kalind.searchanywhere.domain.repo.AppsRepository
import javax.inject.Inject

typealias AppItems = Sequence<WeightedItem<AppItem>>

class GetAppsUseCase @Inject constructor(appsRepository: AppsRepository) {

    private val _filter: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    val filteredApps: Flow<WorkResult<AppItems>> =
        appsRepository.availableApps().combine(_filter) { apps, filter ->
            apps.map { appListData ->
                val appItems = appListData.asSequence().map(AppItem::fromData)
                filterItems(appItems, filter)
            }
        }

    fun setFilter(filter: List<String>) {
        _filter.value = filter
    }
}
