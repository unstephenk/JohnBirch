import org.jetbrains.kotlin.gradle.dsl.JvmTarget // Add this import

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" // Keep explicit KSP version if needed, or alias it

}

// Move kotlin block outside of android block
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11 // Use the new compilerOptions DSL
    }
}

android {
    namespace = "com.kuehlconsulting.johnbirchsociety"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kuehlconsulting.johnbirchsociety"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)

    // Using the specific version aliases you provided for consistency
    implementation(libs.androidx.lifecycle.viewmodel.compose.v285)
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // For ViewModel helper methods
    implementation(libs.androidx.lifecycle.runtime.ktx.v285)

    // Media3 dependencies - ensure no duplicates and use compose-specific UI if needed
    implementation(libs.androidx.media3.exoplayer) // Media3 ExoPlayer
    implementation(libs.androidx.media3.session)   // For media session and background playback
    implementation(libs.androidx.media3.ui.compose) // For Compose UI integration (if you need default controls)
    // If you are NOT using Compose UI for playback controls, use:
    implementation(libs.androidx.media3.ui) // For View-based default UI controls
    implementation(libs.androidx.media3.common)    // Generally pulled in by others, but explicit is fine

    // For file download
    implementation(libs.okhttp)

    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
