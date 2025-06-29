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
        // THIS REPOSITORY IS ESSENTIAL for the sticky library to be found
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MyLog"
include(":app")