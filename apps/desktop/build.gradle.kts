import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(project(":hostess-core"))
    implementation(project(":hostess-ui"))
    implementation(project(":hostess-preferences"))
    implementation(project(":hostess-credential-vault"))
    implementation(project(":hostess-protocol-libomv"))
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test-junit"))
}

compose.desktop {
    application {
        mainClass = "org.hostess.apps.desktop.HostessDesktopAppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "hostess"
            packageVersion = "0.1.8"
            description = "Second Life venue notice helper"
            vendor = "Ella Hostess"
        }
    }
}
