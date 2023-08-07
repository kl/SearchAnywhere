package se.kalind.searchanywhere.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.AppItemData
import se.kalind.searchanywhere.domain.AppsRepository
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.AppItem

@OptIn(DelicateCoroutinesApi::class)
class DefaultAppsRepository(
    private val context: Context,
    private val appHistoryDao: AppHistoryDao,
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
                    GlobalScope.launch(Dispatchers.Default) {
                        appHistoryDao.deleteFromHistory(entity)
                    }
                    return@mapNotNull null
                }
                val item = AppItemData(
                    id = entity.id,
                    label = entity.label,
                    packageName = entity.packageName,
                    activityName = entity.activityName,
                    icon = resolveInfo.loadIcon(context.packageManager),
                )
                Pair(item, entity.updateTime)
            }
        }
    }

    override fun saveToHistory(item: AppItem) {
        val entity = AppHistoryEntity(
            id = item.id,
            label = item.label,
            packageName = item.packageName,
            activityName = item.activityName,
            updateTime = System.currentTimeMillis(),
        )
        GlobalScope.launch(Dispatchers.Default) {
            appHistoryDao.saveToHistory(entity)
        }
    }

    private fun getApps(): List<AppItemData> {
        val intent = Intent()
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        return context.packageManager.queryIntentActivities(intent, 0)
            .map { info ->
                val label = info.loadLabel(context.packageManager).toString()
                val packageName = info.activityInfo.packageName
                AppItemData(
                    id = label + packageName,
                    label = label,
                    packageName = packageName,
                    activityName = info.activityInfo.name,
                    icon = info.activityInfo.loadIcon(context.packageManager),
                )
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
