package se.kalind.searchanywhere.data.settings

import android.provider.Settings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.repo.SettingItem
import se.kalind.searchanywhere.domain.repo.SettingItemData
import se.kalind.searchanywhere.domain.repo.SettingsRepository
import java.lang.reflect.Field

@OptIn(DelicateCoroutinesApi::class)
class DefaultSettingsRepository(
    private val settingHistoryDao: SettingHistoryDao,
) : SettingsRepository {

    override fun availableSettings(): Flow<WorkResult<List<SettingItemData>>> {
        return flow {
            val result = try {
                WorkResult.Success(getSettings())
            } catch (e: SecurityException) {
                WorkResult.Error(e, "Error: failed to read settings")
            }
            emit(result)
        }
    }

    override fun history(): Flow<List<Pair<SettingItemData, UnixTimeMs>>> {
        return settingHistoryDao.getLatestHistory().map { history ->
            history.map { hist ->
                val item = SettingItemData(
                    id = hist.id,
                    fieldName = hist.fieldName,
                    fieldValue = hist.fieldValue,
                )
                Pair(item, hist.updateTime)
            }
        }
            .flowOn(Dispatchers.IO)
    }

    override fun saveToHistory(item: SettingItem) {
        GlobalScope.launch(Dispatchers.IO) {
            settingHistoryDao.saveToHistory(item.toEntity())
        }
    }

    override fun deleteFromHistory(item: SettingItem) {
        GlobalScope.launch(Dispatchers.IO) {
            settingHistoryDao.deleteFromHistory(item.toEntity())
        }
    }

    private fun getSettings(): List<SettingItemData> {
        return staticFields(Settings::class.java)
            .filter { field -> field.name.startsWith("ACTION_") }
            .map { field ->
                val fieldValue = field.get(Settings::class.java) as String
                SettingItemData(
                    id = field.name,
                    fieldName = field.name,
                    fieldValue = fieldValue
                )
            }
    }

    private fun <T> staticFields(className: Class<T>): List<Field> {
        return className.declaredFields
            .filter { f ->
                java.lang.reflect.Modifier.isStatic(f.modifiers)
            }
    }
}

private fun SettingItem.toEntity(): SettingHistoryEntity {
    return SettingHistoryEntity(
        id = id,
        fieldName = fieldName,
        fieldValue = fieldValue,
        updateTime = System.currentTimeMillis(),
    )
}
