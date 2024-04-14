package se.kalind.searchanywhere.domain.usecases

import android.util.Log
import se.kalind.searchanywhere.domain.repo.FileItem
import java.util.regex.Pattern

private val WHITESPACE = Pattern.compile("\\s+")

fun <T : DisplayName> filterItems(items: Sequence<T>, queries: List<String>): Sequence<WeightedItem<T>> {
    return if (queries.isEmpty() || queries.first().isEmpty()) {
        emptySequence()
    } else {
        items.filter { item ->
            queries.all { item.displayName.contains(it, ignoreCase = true) }
        }.map { item ->
            weigh(item, queries.first())
        }
    }
}

fun weighFiles(items: Sequence<FileItem>, queries: List<String>): Sequence<WeightedItem<FileItem>> {
    return if (queries.isEmpty() || queries.first().isEmpty()) {
        emptySequence()
    } else {
        items.map { weigh(it, queries.first()) }
    }
}

private fun <T : DisplayName> weigh(item: T, filter: String): WeightedItem<T> {
    var weight = 0
    // Name starts with search
    if (item.displayName.startsWith(filter, ignoreCase = true)) {
        weight += 2
    }
    // Search matches a word in name
    val parts = item.displayName.split(WHITESPACE)
    if (parts.any { it.equals(filter, ignoreCase = true) }) {
        weight += 1
    }
    return WeightedItem(weight, item)
}

data class WeightedItem<T>(val weight: Int, val item: T)

interface DisplayName {
    val displayName: String
}
