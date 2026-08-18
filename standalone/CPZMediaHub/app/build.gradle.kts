plugins {
    id("com.android.application")
}

val releaseKeystore = System.getenv("CPZ_MEDIA_HUB_KEYSTORE")
val releaseStorePassword = System.getenv("CPZ_MEDIA_HUB_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("CPZ_MEDIA_HUB_KEY_ALIAS")
val releaseKeyPassword = System.getenv("CPZ_MEDIA_HUB_KEY_PASSWORD")
val hasLocalReleaseSigning = listOf(
    releaseKeystore,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.cpozom.mediahub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cpozom.mediahub"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasLocalReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystore!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasLocalReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    // Intentionally empty. Permanent CPZ Media Hub uses Android platform APIs only.
}
