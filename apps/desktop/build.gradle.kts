plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    application
}

dependencies {
    implementation(project(":hostess-core"))
    implementation(project(":hostess-ui"))
    implementation(project(":hostess-credential-vault"))
    implementation(project(":hostess-protocol-libomv"))
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test-junit"))
}

application {
    mainClass.set("org.hostess.apps.desktop.HostessDesktopAppKt")
}
