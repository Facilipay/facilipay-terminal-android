plugins {
    id("com.android.application")
}

android {
    namespace = "io.facilipay.terminal.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.facilipay.terminal.sample"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("io.facilipay:terminal-sdk:${providers.gradleProperty("terminalSdkVersion").getOrElse("1.0.0")}")
    implementation("androidx.appcompat:appcompat:1.7.1")
}
