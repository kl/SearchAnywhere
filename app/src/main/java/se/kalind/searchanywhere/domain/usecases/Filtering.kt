package se.kalind.searchanywhere.domain.usecases

import java.util.regex.Pattern

private val WHITESPACE = Pattern.compile("\\s+")

fun <T : DisplayName> filterItems(items: Sequence<T>, filter: String): Sequence<WeightedItem<T>> {
    return if (filter.isEmpty()) {
        emptySequence()
    } else {
        val filterLower = filter.lowercase()
        items.filter { item ->
            item.displayName.contains(filterLower, ignoreCase = true)
        }.map { item ->
            var weight = 0
            val name = item.displayName.lowercase()
            // Name starts with search
            if (name.startsWith(filterLower)) {
                weight += 1
            }
            // Search matches a word in name
            if (name.split(WHITESPACE).contains(filterLower)) {
                weight += 1
            }
            WeightedItem(weight, item)
        }
    }
}

data class WeightedItem<T>(val weight: Int, val item: T)

interface DisplayName {
    val displayName: String
}
