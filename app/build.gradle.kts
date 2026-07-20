plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "yaroslavalexashkin.cheburping"
    compileSdk {
        version = release(36) {
            //minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "yaroslavalexashkin.cheburping"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    enableKotlin = false
}

dependencies {
    //testImplementation(libs.junit)
}
