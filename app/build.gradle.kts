import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") // AGP 9 has Kotlin support built in
}

// Release signing comes from environment variables (set by the release workflow from
// repository secrets). Without them, release builds are simply left unsigned.
val signingStoreFile: String? = System.getenv("SIGNING_STORE_FILE")

android {
    namespace = "com.toktiklite"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.toktiklite"
        minSdk = 26          // Android 8.0+
        targetSdk = 35
        // CI passes these (-PversionCode=… -PversionName=…); local builds use the defaults.
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (signingStoreFile != null) {
            create("release") {
                storeFile = file(signingStoreFile)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingStoreFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        animationsDisabled = true
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // Lint runs as its own CI step and its report is uploaded; it doesn't block builds.
        abortOnError = false
        checkReleaseBuilds = false
        // Play Store target-SDK deadlines don't apply to a sideloaded app.
        disable += "ExpiredTargetSdkVersion"
        // Plain-text report; CI prints it into the build log.
        textReport = true
        textOutput = file("build/reports/lint-results-debug.txt")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // The app itself has no dependencies: just Android's own WebView and views.

    // Test-only libraries: used to build and run the tests, never included in the app APK.
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
