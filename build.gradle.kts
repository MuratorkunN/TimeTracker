// build.gradle.kts (RoboticsGenius)

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

// NOTE: The allprojects{} and buildscript{} blocks have been completely removed.
// Their configuration is now handled correctly in settings.gradle.kts.