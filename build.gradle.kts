plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.kapt") version "2.0.21"
}

android {
    namespace = "com.personalradar.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.personalradar.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("devDebug") {
            storeFile = file("signing/personalradar-debug.keystore")
            storePassword = "personalradar"
            keyAlias = "personalradar-debug"
            keyPassword = "personalradar"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("devDebug")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val roomVersion = "2.6.1"
    val lifecycleVersion = "2.8.7"

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("com.alphacephei:vosk-android:0.3.75")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
