package se.kalind.searchanywhere.data.files

class AnlocateLibrary {
    companion object {
        init {
            System.loadLibrary("anlocate")
        }
    }

    external fun nativeBuildDatabase(dbFile: String, scanRoot: String, tempDir: String)
    external fun nativeFindFiles(dbFile: String, query: Array<String>): Array<String>
}
