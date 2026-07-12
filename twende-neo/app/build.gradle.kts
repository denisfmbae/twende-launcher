plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "co.nedlink.twende"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.nedlink.twende"
        minSdk = 28            // covers Android 9/10 budget head units
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Signing is driven by environment variables so CI (and your own machine) can
    // produce an installable release APK without a keystore ever entering the repo.
    // Unset => release stays unsigned and only assembleDebug is installable.
    signingConfigs {
        create("sideload") {
            val ks = System.getenv("TWENDE_KEYSTORE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("TWENDE_STORE_PASS")
                keyAlias = System.getenv("TWENDE_KEY_ALIAS")
                keyPassword = System.getenv("TWENDE_KEY_PASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("TWENDE_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("sideload")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.datastore)
    implementation(libs.coroutines.android)
    implementation(libs.play.location)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.compose.tooling)
}
