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
        // 24 = Android 7.0. Budget head units very commonly run 7.1/8.1/9 while the
        // box just says "Android System"; an APK with a higher floor fails on them
        // with the unhelpful "There was a problem parsing the package" dialog.
        minSdk = 24
        targetSdk = 34
        versionCode = 11
        versionName = "2.4"
    }

    // Signing is driven by environment variables so CI (and your own machine) can
    // produce an installable release APK without a keystore ever entering the repo.
    // Unset => release stays unsigned and only assembleDebug is installable.
    signingConfigs {
        create("sideload") {
            // Emit both schemes: v2 covers Android 7+, v1 keeps the oldest
            // installers happy. Costs a few KB.
            enableV1Signing = true
            enableV2Signing = true
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
        // Back-ports java.time to API < 26 (the clock uses LocalDateTime).
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
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
