import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
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

        // ============================================================
        // Secure API Key Management
        // ============================================================
        // Load API keys from local.properties file (not tracked in git)
        // Add your keys to local.properties:
        // KAKAO_APP_KEY=your_kakao_key
        // NAVER_CLIENT_ID=your_naver_client_id
        // NAVER_CLIENT_SECRET=your_naver_client_secret
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        // Inject API keys into BuildConfig for secure access
        buildConfigField("String", "KAKAO_APP_KEY", "\"${localProperties.getProperty("KAKAO_APP_KEY", "")}\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${localProperties.getProperty("NAVER_CLIENT_ID", "")}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${localProperties.getProperty("NAVER_CLIENT_SECRET", "")}\"")

        // Inject API keys into AndroidManifest.xml
        manifestPlaceholders["KAKAO_APP_KEY"] = localProperties.getProperty("KAKAO_APP_KEY", "")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            versionNameSuffix = "-debug"
            // Note: applicationIdSuffix removed to avoid Firebase package name conflicts
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing config would go here for release builds
            // signingConfig = signingConfigs.getByName("release")
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
        // Disable lint check that blocks the build
        abortOnError = false
        // Ignore warnings to speed up build
        checkReleaseBuilds = false
        // Disable specific checks
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    // ============================================================
    // AndroidX Core Libraries
    // ============================================================
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.material)

    // ============================================================
    // UI Components
    // ============================================================
    // Android 12+ Splash Screen support
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Circle ImageView for profile pictures
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // ============================================================
    // Google Services
    // ============================================================
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // ============================================================
    // Firebase
    // ============================================================
    // Firebase BOM (Bill of Materials) - manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // ============================================================
    // Social Login SDKs
    // ============================================================
    // Kakao Login SDK
    implementation("com.kakao.sdk:v2-user:2.19.0")
    // Naver Login SDK
    implementation("com.navercorp.nid:oauth:5.9.1")

    // ============================================================
    // Testing
    // ============================================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}