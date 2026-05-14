plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
}

android {
    namespace = "com.freeze1188.smarthold"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freeze1188.smarthold"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-flat-test"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs(".")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
