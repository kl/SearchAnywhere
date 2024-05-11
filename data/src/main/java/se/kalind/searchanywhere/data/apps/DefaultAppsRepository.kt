package se.kalind.searchanywhere.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.AppIconDrawable
import se.kalind.searchanywhere.domain.repo.AppItem
import se.kalind.searchanywhere.domain.repo.AppItemData
import se.kalind.searchanywhere.domain.repo.AppsRepository

class DefaultAppsRepository(
    private val context: Context,
    private val appHistoryDao: AppHistoryDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope,
) : AppsRepository {

    override fun availableApps(): Flow<WorkResult<List<AppItemData>>> {
        return flow {
            val result = try {
                WorkResult.Success(getApps())
            } catch (e: SecurityException) {
                WorkResult.Error(e, "failed to read apps")
            }
            emit(result)
        }
    }

    override fun history(): Flow<List<Pair<AppItemData, UnixTimeMs>>> {
        return appHistoryDao.getLatestHistory().map { history ->
            history.mapNotNull { entity ->
                val resolveInfo = resolveActivity(context, entity.packageName, entity.activityName)
                if (resolveInfo == null) {
                    // If the application was uninstalled it is no longer resolvable so we filter it
                    // out and remove it from the history DB.
                    appHistoryDao.deleteFromHistory(entity)
                    return@mapNotNull null
                }
                val item = AppItemData(
                    id = entity.id,
                    label = entity.label,
                    packageName = entity.packageName,
                    activityName = entity.activityName,
                    icon = AppIconDrawable(resolveInfo.loadIcon(context.packageManager)),
                )
                Pair(item, entity.updateTime)
            }
        }
            .flowOn(ioDispatcher)
    }

    override fun saveToHistory(item: AppItem) {
        appScope.launch(ioDispatcher) {
            appHistoryDao.saveToHistory(item.toEntity())
        }
    }

    override fun deleteFromHistory(item: AppItem) {
        appScope.launch(ioDispatcher) {
            appHistoryDao.deleteFromHistory(item.toEntity())
        }
    }

    private fun getApps(): List<AppItemData> {
        val intent = Intent()
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val seenIds = mutableMapOf<String, Boolean>()
        return context.packageManager.queryIntentActivities(intent, 0)
            .map { info ->
                val label = info.loadLabel(context.packageManager).toString()
                val packageName = info.activityInfo.packageName
                AppItemData(
                    id = label + packageName,
                    label = label,
                    packageName = packageName,
                    activityName = info.activityInfo.name,
                    icon = AppIconDrawable(info.activityInfo.loadIcon(context.packageManager)),
                )
            }
            .filter { info ->
                // some devices will return duplicate entries so filter out if id already exists.
                if (seenIds.containsKey(info.id)) {
                    false
                } else {
                    seenIds[info.id] = true
                    true
                }
            }
    }

    private fun resolveActivity(
        context: Context,
        packageName: String,
        activityName: String
    ): ResolveInfo? {
        val intent = Intent()
        intent.component = ComponentName(packageName, activityName)
        return context.packageManager.resolveActivity(intent, 0)
    }
}

private fun AppItem.toEntity(): AppHistoryEntity {
    return AppHistoryEntity(
        id = id,
        label = label,
        packageName = packageName,
        activityName = activityName,
        updateTime = System.currentTimeMillis(),
    )
}
