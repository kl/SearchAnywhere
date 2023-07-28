package se.kalind.searchanywhere.domain

import se.kalind.searchanywhere.domain.usecases.AppItem
import se.kalind.searchanywhere.domain.usecases.SettingItem

sealed class ItemType {
    data class Setting(val item: SettingItem) : ItemType()
    data class App(val item: AppItem) : ItemType()
}