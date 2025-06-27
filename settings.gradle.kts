// settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // This repository is needed for MPAndroidChart AND the color picker
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MyLog" // Changed this in a previous step, just confirming
include(":app")