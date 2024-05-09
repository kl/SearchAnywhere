package se.kalind.searchanywhere.presentation

data class Loading<T>(val data: T?) {
    fun isLoading(): Boolean = data == null
    fun hasLoaded(): Boolean = !isLoading()
}