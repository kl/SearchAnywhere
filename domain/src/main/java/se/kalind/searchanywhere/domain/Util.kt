package se.kalind.searchanywhere.domain

typealias UnixTimeMs = Long

fun <T> Result.Companion.err(message: String): Result<T> {
    return failure(IllegalStateException(message))
}

fun Result.Companion.ok(): Result<Unit> {
    return success(Unit)
}

fun <T> Result<T>.messageOrNull(): String? {
    return exceptionOrNull()?.message
}