# SearchAnywhere

Android app to quickly find and open settings, apps and files.

Uses a file path database similar to the Linux 'locate' command to speed up file searches.

![app screenshot](screenshot.png)

## Architecture

The app is made up by 4 gradle modules: **App**, **Presentation**, **Data** and **Domain**.

The **App** module contains the Application class and main AndroidManifest.xml file.
It has a dependency on **Presentation** and **Data** so that all modules are included in the final 
dependency graph.

The **Presentation** module is an Android library that contains the main activity class and all
UI related classes. It only depends on the **Domain** module.

The **Data** module is an Android library that provides the data sources for files, apps and settings.
It only depends on the **Domain** module.

Finally, the **Domain** module is a **Kotlin library** that contains large parts of the core app 
logic, for example the search order filtering algorithm. It does not depend on any other module.

![app architecture diagram](structure.png)

__Uses__:

* Compose
* View models
* Kotlin Flows
* Room
* Hilt dependency injection

## Building the Rust library

Note that building the Rust library is not required to build the app in Android studio as the
compiled native libraries are checked in to the repo.

To build the Rust library to the following :

* Install the latest stable Rust with rustup (https://www.rust-lang.org/tools/install)
* Install the Android NDK version 25.1.8937393 (Android Studio -> Settings -> Android SDK -> SDK
  Tools -> Show Package Details -> NDK (Side by side) -> 25.1.8937393)
* Add targets with rustup:

```
rustup target add \
  aarch64-linux-android \
  armv7-linux-androideabi \
  i686-linux-android \
  x86_64-linux-android
```

* Add the following to `~/.cargo/config.toml` (replace NDK_INSTALL_DIR with the absolute path of the NDK
  directory):

```
[target.aarch64-linux-android]
linker = "NDK_INSTALL_DIR/25.1.8937393/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang"

[target.armv7-linux-androideabi]
linker = "NDK_INSTALL_DIR/25.1.8937393/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi30-clang"

[target.i686-linux-android]
linker = "NDK_INSTALL_DIR/25.1.8937393/toolchains/llvm/prebuilt/linux-x86_64/bin/i686-linux-android30-clang"

[target.x86_64-linux-android]
linker = "NDK_INSTALL_DIR/25.1.8937393/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android30-clang"
```

* Run the tests: `cd anlocate && cargo test`
* Build all native libs and copy to jniLibs dir: `./gradlew copyAnlocateLibs`
