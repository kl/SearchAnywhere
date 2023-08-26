package se.kalind.searchanywhere.domain

import se.kalind.searchanywhere.domain.repo.AppItem
import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.SettingItem

sealed class ItemType {
    data class Setting(val item: SettingItem) : ItemType()
    data class App(val item: AppItem) : ItemType()
    data class File(val item: FileItem) : ItemType()
}

interface ToItemType {
    fun toItemType(): ItemType
}