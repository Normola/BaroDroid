// Release builds take their version and signing key from the environment, so the
// CI release job can stamp a tag onto the APK without the repo carrying secrets.
val releaseVersionName = providers.environmentVariable("BARODROID_VERSION_NAME").orNull
val releaseVersionCode = providers.environmentVariable("BARODROID_VERSION_CODE").orNull?.toIntOrNull()
val releaseKeystore = providers.environmentVariable("BARODROID_KEYSTORE_FILE").orNull

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.normola.barodroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.normola.barodroid"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseVersionCode ?: 1
        versionName = releaseVersionName ?: "1.0"
    }

    signingConfigs {
        if (!releaseKeystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("BARODROID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("BARODROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("BARODROID_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            // Without a keystore in the environment the build still produces an
            // installable APK, signed with the debug key; the release notes say so.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
}
