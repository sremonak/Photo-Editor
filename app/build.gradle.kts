plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Monotonic versioning: every CI build increments versionCode so a new APK
// always installs as an in-place update over any previously installed release.
val ciRunNum = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val computedVersionCode = 100 + (ciRunNum ?: 0)
val computedVersionName = "1.0.${ciRunNum ?: 0}"

android {
    namespace = "com.lumacraft.studio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lumacraft.studio"
        minSdk = 26
        targetSdk = 35
        versionCode = computedVersionCode
        versionName = computedVersionName
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val customPath = System.getenv("LUMA_KEYSTORE_PATH")
            storeFile = if (!customPath.isNullOrBlank()) file(customPath) else file("keystore/lumacraft-release.p12")
            storeType = "PKCS12"
            storePassword = System.getenv("LUMA_STORE_PASSWORD") ?: "LumaCraftRelease2026"
            keyAlias = System.getenv("LUMA_KEY_ALIAS") ?: "lumacraft_release_key"
            keyPassword = System.getenv("LUMA_KEY_PASSWORD") ?: "LumaCraftRelease2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Same identity + signature as release: installs update in-place, never conflicts.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    // Per-ABI APK splits: dramatically smaller downloads (native libs are per-ABI).
    // A universal APK is still produced so any install path keeps working.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    // On-device ML Kit Selfie Segmentation (bundled model, 100% offline, Apache 2.0)
    implementation(libs.mlkit.segmentation.selfie)
    implementation(libs.kotlinx.coroutines.play.services)
    // CameraX capture module
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    debugImplementation(libs.androidx.ui.tooling)
}
