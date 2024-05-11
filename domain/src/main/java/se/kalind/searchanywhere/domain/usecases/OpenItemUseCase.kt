package se.kalind.searchanywhere.domain.usecases

import se.kalind.searchanywhere.domain.ItemType
import javax.inject.Inject

interface ItemOpener {
    fun openItem(item: ItemType): Result<Unit>
}

class OpenItemUseCase @Inject constructor(private val opener: ItemOpener) {
    fun openItem(item: ItemType): Result<Unit> = opener.openItem(item)
}
