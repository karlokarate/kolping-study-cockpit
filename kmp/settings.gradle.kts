rootProject.name = "kolping-study-cockpit"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

include(":shared")
include(":androidApp")
include(":mapping:core")
include(":mapping:android")
include(":recorder:app")
