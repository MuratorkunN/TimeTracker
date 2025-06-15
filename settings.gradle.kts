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
        // This repository is needed for MPAndroidChart
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "RoboticsGenius"
include(":app")