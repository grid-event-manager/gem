import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(project(":gem-core"))
    implementation(project(":gem-ui"))
    implementation(project(":gem-preferences"))
    implementation(project(":gem-credential-vault"))
    implementation(project(":gem-protocol-libomv"))
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test-junit"))
}

compose.desktop {
    application {
        mainClass = "org.gem.apps.desktop.HostessDesktopAppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "hostess"
            packageVersion = "0.1.10"
            description = "Second Life venue notice helper"
            vendor = "Grid Event Manager"
        }
    }
}
