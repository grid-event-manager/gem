plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.hostess.apps.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.hostess.apps.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.1.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "Ella Hostess"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":hostess-core"))
    implementation(project(":hostess-ui"))
    implementation(project(":hostess-preferences"))
    implementation(project(":hostess-credential-vault"))
    implementation(project(":hostess-protocol-libomv"))
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(project(":hostess-credential-vault"))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    testImplementation(kotlin("test-junit"))
}
