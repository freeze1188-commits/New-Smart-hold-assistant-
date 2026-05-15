plugins {
    id("com.android.application") version "8.7.3"
}

android {
    namespace = "com.freeze1188.smarthold"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freeze1188.smartholdjava"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.0-java-test"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("generated-src")
        }
    }
}
