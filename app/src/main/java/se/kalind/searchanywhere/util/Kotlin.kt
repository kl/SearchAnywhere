package se.kalind.searchanywhere.util

/** Filters and maps at the same time. If null is returned from transform the
element is filtered out **/
inline fun <T, R> Iterable<T>.filterMap(transform: (T) -> R?): List<R> {
    val filtered = mutableListOf<R>()
    for (element in this) {
        val transformed = transform(element)
        if (transformed != null) {
            filtered.add(transformed)
        }
    }
    return filtered
}
