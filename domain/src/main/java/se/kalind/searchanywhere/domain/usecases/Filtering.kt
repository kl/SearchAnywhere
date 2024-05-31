package se.kalind.searchanywhere.domain.usecases

import se.kalind.searchanywhere.domain.repo.FileItem
import se.kalind.searchanywhere.domain.repo.MatchType
import se.kalind.searchanywhere.domain.repo.SearchQuery
import java.util.regex.Pattern

private const val INCLUDE_CHARACTER = "&"
private const val EXCLUDE_CHARACTER = "!"

private val WHITESPACE = Pattern.compile("\\s+")
private val INCLUDE_DELIMITER = Pattern.compile("""(?<!\\)$INCLUDE_CHARACTER""")
private val EXCLUDE_DELIMITER = Pattern.compile("""(?<!\\)$EXCLUDE_CHARACTER""")

fun <T : DisplayName> filterItems(
    items: Sequence<T>,
    queries: List<SearchQuery>
): Sequence<WeightedItem<T>> {
    return if (queries.isEmpty() || queries.first().query.isEmpty()) {
        emptySequence()
    } else {
        items.filter { item ->
            queries.all { query ->
                val contains = item.displayName.contains(query.query, ignoreCase = true)
                if (query.matchType == MatchType.INCLUDE) {
                    contains
                } else {
                    !contains
                }
            }
        }.map { item ->
            weigh(item, queries.first().query)
        }
    }
}

fun weighFiles(items: Sequence<FileItem>, queries: List<SearchQuery>): Sequence<WeightedItem<FileItem>> {
    return if (queries.isEmpty() || queries.first().query.isEmpty()) {
        emptySequence()
    } else {
        items.map { weigh(it, queries.first().query) }
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

// Split input on '&' (include) and '!' (exclude). Escape with '\&' and '\!'.
fun splitFilter(filter: String): List<SearchQuery> {
    if (!filter.contains(INCLUDE_CHARACTER) && !filter.contains(EXCLUDE_CHARACTER)) {
        return listOf(SearchQuery(query = filter, matchType = MatchType.INCLUDE))
    }

    // e.g. "lol&lmao!tldr!stfu&cool" becomes:
    // [lol->true, lmao->true, tldr->false, stfu->false, cool->true]
    val includes = filter.split(INCLUDE_DELIMITER)
        .map { it.replace("\\$INCLUDE_CHARACTER", INCLUDE_CHARACTER) }

    return includes.flatMap { include ->
        val split = include.split(EXCLUDE_DELIMITER)
            .map { it.replace("\\$EXCLUDE_CHARACTER", EXCLUDE_CHARACTER) }

        split.withIndex().map { (index, s) ->
            SearchQuery(
                query = s,
                matchType = if (index == 0) MatchType.INCLUDE else MatchType.EXCLUDE
            )
        }
        .filter { it.query.isNotEmpty() }
    }
}
