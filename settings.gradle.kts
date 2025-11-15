pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io") // ✅ Added for UCrop and other GitHub dependencies
    }
}

dependencyResolutionManagement {
    // Prevent repositories from being declared in individual build.gradle files
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // ✅ Required for UCrop
    }
}

rootProject.name = "OutPick"
include(":app")
