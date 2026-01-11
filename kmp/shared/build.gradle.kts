plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                
                // Ktor Client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                
                // HTML Parsing
                implementation(libs.ksoup)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
            }
        }
    }
}

android {
    namespace = "de.kolping.cockpit.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 26
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
