package se.kalind.searchanywhere.data.settings

import android.provider.Settings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import se.kalind.searchanywhere.domain.SettingItemData
import se.kalind.searchanywhere.domain.SettingsRepository
import se.kalind.searchanywhere.domain.UnixTimeMs
import se.kalind.searchanywhere.domain.WorkResult
import se.kalind.searchanywhere.domain.usecases.SettingItem
import java.lang.reflect.Field

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
    }

    override fun saveToHistory(item: SettingItem) {
        val entity = SettingHistoryEntity(
            id = item.id,
            fieldName = item.fieldName,
            fieldValue = item.fieldValue,
            updateTime = System.currentTimeMillis(),
        )
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.Default) {
            settingHistoryDao.saveToHistory(entity)
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