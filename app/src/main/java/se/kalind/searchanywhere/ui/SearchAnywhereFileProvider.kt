package se.kalind.searchanywhere.ui

import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import se.kalind.searchanywhere.R

class SearchAnywhereFileProvider : FileProvider(R.xml.provider_paths) {
    companion object {
        val AUTHORITY = "se.kalind.searchanywhere.fileprovider"
    }
}