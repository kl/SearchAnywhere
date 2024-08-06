
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.library) apply false
    id("se.kalind.searchanywhere.gradle.anlocate-plugin")
}

// TODO: figure out how to apply the plugin without deprecations
apply<se.kalind.searchanywhere.gradle.Anlocate_plugin_gradle.AnlocatePlugin>()

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}
