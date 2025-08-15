// app/build.gradle.kts  (replace the broken one)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

android {
    namespace = "com.kuehlconsulting.johnbirchsociety"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kuehlconsulting.johnbirchsociety"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Make Java match Kotlin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

    dependencies {
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.tooling.preview)
        implementation(libs.androidx.activity.compose)

        implementation(libs.androidx.lifecycle.runtime.ktx)

        implementation(libs.androidx.media3.exoplayer)
        implementation(libs.androidx.media3.session)
        implementation(libs.androidx.media3.ui) // we’ll use PlayerControlView inside Compose

        implementation(libs.kotlinx.coroutines.android)

        implementation(libs.okhttp)

        implementation(libs.room.runtime)
        implementation(libs.room.ktx)
        ksp(libs.room.compiler)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.android)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.xml)

        implementation(libs.androidx.lifecycle.viewmodel.compose)

    }
