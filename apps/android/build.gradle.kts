plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.hostess.apps.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.hostess.apps.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":hostess-core"))
    implementation(project(":hostess-protocol-libomv"))
    testImplementation(kotlin("test-junit"))
}
