// Top-level build.gradle.kts (Project level)

plugins {
    // Android & Kotlin
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false

    // Google Services (Firebase, etc.)
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Google Services classpath (for Firebase, etc.)
        classpath("com.google.gms:google-services:4.4.2")
    }
}
