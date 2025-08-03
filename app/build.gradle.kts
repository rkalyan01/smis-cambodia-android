plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.android.compose)
}

android {
    namespace = "com.innovative.smis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.innovative.smis"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://smis-11.innovativesolution.com.np/api/\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Ensure this is compatible with your Compose BOM
    }
}

dependencies {

    // Use the Compose BOM to manage versions of Compose-related libraries
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core & UI (Jetpack Compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.compose.shimmer)

    // Compose UI (versions managed by BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Dependency Injection
    implementation(libs.koin.androidx.compose)

    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.adapter.rxjava3)
    implementation(libs.logging.interceptor)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:android-maps-utils:3.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")



    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Payment SDKs
    implementation(libs.checkout.android) // Khalti

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation(libs.androidx.ui.test.manifest)
}
