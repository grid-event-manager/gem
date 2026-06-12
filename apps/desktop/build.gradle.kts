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

val desktopPackageName = "gema"
val desktopPackageVersion = "0.1.15"
val debArtifact = layout.buildDirectory.file(
    "compose/binaries/main/deb/${desktopPackageName}_${desktopPackageVersion}_amd64.deb",
)

fun runPackageCommand(vararg command: String) {
    val exitCode = ProcessBuilder(*command)
        .inheritIO()
        .start()
        .waitFor()
    require(exitCode == 0) { "Package command failed ($exitCode): ${command.joinToString(" ")}" }
}

compose.desktop {
    application {
        mainClass = "org.gem.apps.desktop.GemDesktopAppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = desktopPackageName
            packageVersion = desktopPackageVersion
            description = "Second Life venue notice helper"
            vendor = "Grid Event Manager"

            linux {
                iconFile.set(project.file("src/main/package/icons/gema.png"))
                shortcut = true
            }

            windows {
                iconFile.set(project.file("src/main/package/icons/gema.ico"))
                menu = true
                shortcut = true
            }

            macOS {
                iconFile.set(project.file("src/main/package/icons/gema.icns"))
                packageName = desktopPackageName
                dockName = "GEM"
                bundleID = "org.gem.apps.desktop"
            }
        }
    }
}

tasks.configureEach {
    if (name != "packageDeb") {
        return@configureEach
    }

    doLast {
        val debFile = debArtifact.get().asFile
        require(debFile.isFile) { "Expected deb artifact missing: ${debFile.absolutePath}" }

        val workDir = layout.buildDirectory.dir("tmp/packageDebWithCommandLauncher").get().asFile
        delete(workDir)
        workDir.mkdirs()

        runPackageCommand("dpkg-deb", "-R", debFile.absolutePath, workDir.absolutePath)

        val commandLink = workDir.resolve("usr/bin/$desktopPackageName")
        commandLink.parentFile.mkdirs()
        runPackageCommand("ln", "-sfn", "/opt/$desktopPackageName/bin/$desktopPackageName", commandLink.absolutePath)

        runPackageCommand("dpkg-deb", "--root-owner-group", "-b", workDir.absolutePath, debFile.absolutePath)
    }
}
