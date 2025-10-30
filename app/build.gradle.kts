import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.connectmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.connectmate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Final Attempt: Force include all native libraries for all CPU architectures.
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        buildConfigField("String", "KAKAO_APP_KEY", "\"${localProperties.getProperty("KAKAO_APP_KEY", "")}\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${localProperties.getProperty("NAVER_CLIENT_ID", "")}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${localProperties.getProperty("NAVER_CLIENT_SECRET", "")}\"")

        manifestPlaceholders["KAKAO_APP_KEY"] = localProperties.getProperty("KAKAO_APP_KEY", "")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
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
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.material)

    // UI Components
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Google Services
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")

    // Kakao SDK (Latest versions from official documentation)
    implementation("com.kakao.sdk:v2-user:2.22.0")
    implementation("com.kakao.maps.open:android:2.12.18")

    // Naver Login SDK
    implementation("com.navercorp.nid:oauth:5.9.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}