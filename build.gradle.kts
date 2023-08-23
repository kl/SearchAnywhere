// Root build.gradle.kts
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
            .copyTo(File("app/src/main/jniLibs/arm64-v8a/libanlocate.so"), overwrite = true)

        File("anlocate/target/armv7-linux-androideabi/release/libanlocate.so")
            .copyTo(File("app/src/main/jniLibs/armeabi-v7a/libanlocate.so"), overwrite = true)

        File("anlocate/target/i686-linux-android/release/libanlocate.so")
            .copyTo(File("app/src/main/jniLibs/x86/libanlocate.so"), overwrite = true)

        File("anlocate/target/x86_64-linux-android/release/libanlocate.so")
            .copyTo(File("app/src/main/jniLibs/x86_64/libanlocate.so"), overwrite = true)
    }
}
