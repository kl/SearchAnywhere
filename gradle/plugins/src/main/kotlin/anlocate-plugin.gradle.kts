package se.kalind.searchanywhere.gradle

import org.gradle.kotlin.dsl.support.uppercaseFirstChar

/**
 * Relates the name Rust expects for an architecture (e.g. "aarch64-linux-android")
 * to the name Android expects (e.g. "arm64-v8a").
 */
private data class ArchInfo(
    val rustId: String,
    val androidId: String,
) {
    val buildName: String by lazy {
        val camelCased = rustId.replace(Regex("-(.)")) { match ->
            match.groupValues[1].uppercase()
        }.uppercaseFirstChar()

        "build$camelCased"
    }
}

class AnlocatePlugin : Plugin<Project> {

    private val architectures = listOf(
        ArchInfo(
            rustId = "aarch64-linux-android",
            androidId = "arm64-v8a",
        ),
        ArchInfo(
            rustId = "armv7-linux-androideabi",
            androidId = "armeabi-v7a",
        ),
        ArchInfo(
            rustId = "i686-linux-android",
            androidId = "x86",
        ),
        ArchInfo(
            rustId = "x86_64-linux-android",
            androidId = "x86_64",
        ),
    )

    override fun apply(target: Project) {
        createArchitectureBuildTasks(target)
        createMainBuildTask(target)
    }

    private fun createArchitectureBuildTasks(target: Project) {
        architectures.forEach { arch ->
            target.task<Exec>(arch.buildName) {
                group = "Anlocate"
                description = "Builds the anlocate ${arch.rustId} native library"
                workingDir("anlocate")
                commandLine("cargo", "build", "--target", arch.rustId, "--release")
            }
        }
    }

    private fun createMainBuildTask(target: Project) {
        target.tasks.create("buildAnlocateLibs") {
            group = "Anlocate"
            description =
                "Builds the native anlocate libraries and copies them to the jniLibs folder."
            dependsOn(
                *architectures.map(ArchInfo::buildName).toTypedArray()
            )
            doLast {
                architectures.forEach { arch ->
                    File("anlocate/target/${arch.rustId}/release/libanlocate.so")
                        .copyTo(
                            File("data/src/main/jniLibs/${arch.androidId}/libanlocate.so"),
                            overwrite = true
                        )
                }
            }
        }
    }
}
