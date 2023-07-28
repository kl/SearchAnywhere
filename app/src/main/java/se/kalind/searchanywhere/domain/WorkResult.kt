package se.kalind.searchanywhere.domain

sealed class WorkResult<out R> {
    data class Success<out T>(val data: T) : WorkResult<T>()
    data class Error(val exception: Throwable? = null, val message: String? = null) :
        WorkResult<Nothing>()

    object Loading : WorkResult<Nothing>()

    fun <N> map(transform: (success: R) -> N): WorkResult<N> {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Success -> Success(transform(this.data))
            else -> this as WorkResult<N>
        }
    }
}

