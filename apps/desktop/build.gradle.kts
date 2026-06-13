import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

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

val desktopPackageName = "gem"
val desktopCommandName = "gema"
val desktopPackageVersion = "0.1.17"
val macPackageVersion = "1.0.17"
val windowsDisplayName = "GEM $desktopPackageVersion"
val debArtifact = layout.buildDirectory.file(
    "compose/binaries/main/deb/${desktopPackageName}_${desktopPackageVersion}_amd64.deb",
)
val msiArtifact = layout.buildDirectory.file(
    "compose/binaries/main/msi/${desktopPackageName}-${desktopPackageVersion}.msi",
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
            vendor = "ANVLL"

            linux {
                iconFile.set(project.file("src/main/package/icons/gem.png"))
                shortcut = true
            }

            windows {
                iconFile.set(project.file("src/main/package/icons/gem.ico"))
                menu = true
                shortcut = true
                menuGroup = "GEM"
            }

            macOS {
                iconFile.set(project.file("src/main/package/icons/gem.icns"))
                packageName = desktopPackageName
                packageVersion = macPackageVersion
                dmgPackageVersion = macPackageVersion
                dockName = "GEM"
                bundleID = "org.gem.apps.desktop"
            }
        }
    }
}

tasks.configureEach {
    when (name) {
        "packageDeb" -> doLast {
            val debFile = debArtifact.get().asFile
            require(debFile.isFile) { "Expected deb artifact missing: ${debFile.absolutePath}" }

            val workDir = layout.buildDirectory.dir("tmp/packageDebWithCommandLauncher").get().asFile
            delete(workDir)
            workDir.mkdirs()

            runPackageCommand("dpkg-deb", "-R", debFile.absolutePath, workDir.absolutePath)

            val commandLink = workDir.resolve("usr/bin/$desktopPackageName")
            commandLink.parentFile.mkdirs()
            commandLink.delete()

            val commandAlias = workDir.resolve("usr/bin/$desktopCommandName")
            runPackageCommand("ln", "-sfn", "/opt/$desktopPackageName/bin/$desktopPackageName", commandAlias.absolutePath)

            runPackageCommand("dpkg-deb", "--root-owner-group", "-b", workDir.absolutePath, debFile.absolutePath)
        }
        "packageMsi" -> doLast {
            if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                return@doLast
            }
            val msiFile = msiArtifact.get().asFile
            require(msiFile.isFile) { "Expected MSI artifact missing: ${msiFile.absolutePath}" }
            runPackageCommand(
                "cscript.exe",
                "//NoLogo",
                project.file("src/main/package/windows/patch-msi-display.vbs").absolutePath,
                msiFile.absolutePath,
                windowsDisplayName,
            )
        }
    }
}

tasks.withType<AbstractJPackageTask>().configureEach {
    if (targetFormat == TargetFormat.Msi) {
        freeArgs.add("--win-shortcut-prompt")
    }
}
