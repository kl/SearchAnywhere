// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.library) apply false
}

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

listOf(
    "aarch64-linux-android",
    "armv7-linux-androideabi",
    "i686-linux-android",
    "x86_64-linux-android"
).forEach { name ->
    task<Exec>("build_$name") {
        workingDir("anlocate")
        commandLine("cargo", "build", "--target", name, "--release")
    }
}

tasks.register("copyAnlocateLibs") {
    dependsOn(
        "build_aarch64-linux-android",
        "build_armv7-linux-androideabi",
        "build_i686-linux-android",
        "build_x86_64-linux-android",
    )
    doLast {
        File("anlocate/target/aarch64-linux-android/release/libanlocate.so")
            .copyTo(File("data/src/main/jniLibs/arm64-v8a/libanlocate.so"), overwrite = true)

        File("anlocate/target/armv7-linux-androideabi/release/libanlocate.so")
            .copyTo(File("data/src/main/jniLibs/armeabi-v7a/libanlocate.so"), overwrite = true)

        File("anlocate/target/i686-linux-android/release/libanlocate.so")
            .copyTo(File("data/src/main/jniLibs/x86/libanlocate.so"), overwrite = true)

        File("anlocate/target/x86_64-linux-android/release/libanlocate.so")
            .copyTo(File("data/src/main/jniLibs/x86_64/libanlocate.so"), overwrite = true)
    }
}
