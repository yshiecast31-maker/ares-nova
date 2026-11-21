plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.nova.launcher"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.nova.launcher"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.3-ares"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
