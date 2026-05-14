plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
}

android {
    namespace = "com.freeze1188.smarthold"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freeze1188.smartholdtest2"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2-open-test"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("generated-src")
        }
    }
}
