plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.connectmate"
    compileSdk = 35  // Changed from 36 to 35

    defaultConfig {
        applicationId = "com.example.connectmate"
        minSdk = 24
        targetSdk = 35  // Changed from 36 to 35
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
        viewBinding = true
    }
}

dependencies {
    // AndroidX core and appcompat
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Core KTX - Consider moving to version catalog
    implementation("androidx.core:core-ktx:1.13.1")

    // Material Components (Material 3 UI)
    implementation(libs.material)

    // Android 12+ Splash Screen support
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Circle ImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}