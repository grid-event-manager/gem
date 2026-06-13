import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask
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

val desktopPackageName = "gema"
val desktopCommandName = "gema"
val debPackageName = "gema"
val desktopPackageDescription = "Grid Event Manager"
val desktopPackageVersion = "0.1.28"
val macPackageVersion = "1.0.28"
val macApplicationDisplayName = "GEM"
val nativePackageName = if (System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
    macApplicationDisplayName
} else {
    desktopPackageName
}
val windowsDisplayName = "GEM $desktopPackageVersion"
val rawDebArtifact = layout.buildDirectory.file(
    "compose/binaries/main/deb/${desktopPackageName}_${desktopPackageVersion}_amd64.deb",
)
val debArtifact = layout.buildDirectory.file(
    "compose/binaries/main/deb/${debPackageName}_${desktopPackageVersion}_amd64.deb",
)
val msiArtifact = layout.buildDirectory.file(
    "compose/binaries/main/msi/${desktopPackageName}-${desktopPackageVersion}.msi",
)
val rawDmgArtifact = layout.buildDirectory.file(
    "compose/binaries/main/dmg/${macApplicationDisplayName}-${macPackageVersion}.dmg",
)
val dmgArtifact = layout.buildDirectory.file(
    "compose/binaries/main/dmg/${desktopPackageName}-${macPackageVersion}.dmg",
)

fun runPackageCommand(vararg command: String) {
    val exitCode = ProcessBuilder(*command)
        .inheritIO()
        .start()
        .waitFor()
    require(exitCode == 0) { "Package command failed ($exitCode): ${command.joinToString(" ")}" }
}

fun rewriteDebControl(workDir: File) {
    val controlFile = workDir.resolve("DEBIAN/control")
    require(controlFile.isFile) { "Expected deb control file missing: ${controlFile.absolutePath}" }
    val rewritten = controlFile.readLines().map { line ->
        when {
            line.startsWith("Package: ") -> "Package: $debPackageName"
            line.startsWith("Provides: ") -> "Provides: $debPackageName"
            line.startsWith("Description: ") -> "Description: $desktopPackageDescription"
            line.startsWith("Maintainer: ") -> "Maintainer: ANVLL <Unknown>"
            else -> line
        }
    }
    controlFile.writeText(rewritten.joinToString(System.lineSeparator()) + System.lineSeparator())
}

fun rewriteLinuxDesktopEntry(workDir: File) {
    val desktopFile = workDir
        .resolve("opt/$desktopPackageName/lib")
        .walkTopDown()
        .firstOrNull { it.isFile && it.extension == "desktop" }
        ?: return
    val rewritten = desktopFile.readLines().map { line ->
        when {
            line.startsWith("Name=") -> "Name=$windowsDisplayName"
            line.startsWith("Comment=") -> "Comment=$desktopPackageDescription"
            line.startsWith("Exec=") -> "Exec=/usr/bin/$desktopCommandName"
            line.startsWith("Icon=") -> "Icon=/opt/$desktopPackageName/lib/$desktopPackageName.png"
            else -> line
        }
    }
    desktopFile.writeText(rewritten.joinToString(System.lineSeparator()) + System.lineSeparator())
}

fun normalizeMacDmgArtifact() {
    val rawDmgFile = rawDmgArtifact.get().asFile
    val dmgFile = dmgArtifact.get().asFile
    if (rawDmgFile.absolutePath == dmgFile.absolutePath) {
        return
    }
    if (!rawDmgFile.isFile) {
        require(dmgFile.isFile) { "Expected DMG artifact missing: ${rawDmgFile.absolutePath}" }
        return
    }
    dmgFile.delete()
    require(rawDmgFile.renameTo(dmgFile)) {
        "Unable to normalize DMG artifact ${rawDmgFile.absolutePath} to ${dmgFile.absolutePath}"
    }
}

compose.desktop {
    application {
        mainClass = "org.gem.apps.desktop.GemDesktopAppKt"
        if (providers.gradleProperty("gemDiagnosticUdp").orNull == "true") {
            jvmArgs("-Dgem.simulator.udp.diagnostics=true")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = nativePackageName
            packageVersion = desktopPackageVersion
            description = desktopPackageDescription
            vendor = "ANVLL"
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/main/package/app-resources"))

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
                packageName = macApplicationDisplayName
                packageVersion = macPackageVersion
                dmgPackageVersion = macPackageVersion
                dockName = macApplicationDisplayName
                bundleID = "org.gem.apps.desktop"
            }
        }
    }
}

tasks.configureEach {
    when (name) {
        "packageDeb" -> doLast {
            val rawDebFile = rawDebArtifact.get().asFile
            val debFile = debArtifact.get().asFile
            require(rawDebFile.isFile) { "Expected deb artifact missing: ${rawDebFile.absolutePath}" }

            val workDir = layout.buildDirectory.dir("tmp/packageDebWithCommandLauncher").get().asFile
            delete(workDir)
            workDir.mkdirs()

            runPackageCommand("dpkg-deb", "-R", rawDebFile.absolutePath, workDir.absolutePath)
            rewriteDebControl(workDir)
            rewriteLinuxDesktopEntry(workDir)

            val commandLink = workDir.resolve("usr/bin/$desktopPackageName")
            commandLink.parentFile.mkdirs()
            commandLink.delete()

            val commandAlias = workDir.resolve("usr/bin/$desktopCommandName")
            runPackageCommand("ln", "-sfn", "/opt/$desktopPackageName/bin/$desktopPackageName", commandAlias.absolutePath)

            runPackageCommand("dpkg-deb", "--root-owner-group", "-b", workDir.absolutePath, debFile.absolutePath)
            if (rawDebFile.absolutePath != debFile.absolutePath) {
                rawDebFile.delete()
            }
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
                project.file("src/main/package/icons/gem.ico").absolutePath,
                project.file("src/main/package/windows/assets/gem-installer-dialog.bmp").absolutePath,
                project.file("src/main/package/windows/assets/gem-installer-banner.bmp").absolutePath,
            )
        }
        "packageDmg" -> doLast {
            normalizeMacDmgArtifact()
        }
    }
}

tasks.withType<AbstractJPackageTask>().configureEach {
    if (targetFormat == TargetFormat.Msi) {
        freeArgs.add("--win-shortcut-prompt")
    }
}

tasks.withType<AbstractJLinkTask>().configureEach {
    val nativeCommandStripperValue = javaClass.methods
        .firstOrNull { it.name == "getStripNativeCommands\$compose" }
        ?.invoke(this)
    if (nativeCommandStripperValue is org.gradle.api.provider.Property<*>) {
        @Suppress("UNCHECKED_CAST")
        val nativeCommandStripper = nativeCommandStripperValue as org.gradle.api.provider.Property<Boolean>
        nativeCommandStripper.set(false)
    }
}
