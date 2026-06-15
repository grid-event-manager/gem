import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import java.util.Properties

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

sourceSets {
    main {
        resources.srcDir("src/main/package/icons")
    }
}

val desktopPackageName = "gema"
val desktopCommandName = "gema"
val debPackageName = "gema"
val desktopPackageVersion = "0.1.32"
val macPackageVersion = "1.0.32"
val packagingTextProperties = Properties().apply {
    project.file("src/main/package/packaging-text.properties").inputStream().use { input ->
        load(input)
    }
}
fun packagingText(key: String): String =
    packagingTextProperties.getProperty(key) ?: error("Missing packaging text key: $key")
fun versionedPackagingText(key: String, version: String = desktopPackageVersion): String =
    packagingText(key).replace("{version}", version)
val desktopPackageDescription = packagingText("app.fullName")
val macApplicationBundleName = packagingText("mac.bundleName")
val nativePackageName = if (System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
    macApplicationBundleName
} else {
    desktopPackageName
}
val windowsDisplayName = versionedPackagingText("windows.displayName")
val windowsWelcomeTitle = versionedPackagingText("windows.welcomeTitle")
val windowsLaunchAfterInstallText = packagingText("windows.launchAfterInstall")
val windowsDowngradeMessage = packagingText("windows.downgradeMessage")
val windowsRunningInstanceMessage = packagingText("windows.runningInstanceMessage")
val linuxDowngradeMessage = packagingText("linux.downgradeMessage")
val linuxStaleGemInstallMessage = packagingText("linux.staleGemInstallMessage")
val linuxStaleHostessInstallMessage = packagingText("linux.staleHostessInstallMessage")
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
    "compose/binaries/main/dmg/${macApplicationBundleName}-${macPackageVersion}.dmg",
)
val dmgArtifact = layout.buildDirectory.file(
    "compose/binaries/main/dmg/${desktopPackageName}-${macPackageVersion}.dmg",
)

data class WindowsInstallerBitmapAssets(
    val dialog: File,
    val banner: File,
)

fun runPackageCommand(vararg command: String) {
    val exitCode = ProcessBuilder(*command)
        .inheritIO()
        .start()
        .waitFor()
    require(exitCode == 0) { "Package command failed ($exitCode): ${command.joinToString(" ")}" }
}

fun javaExecutable(command: String): String {
    val suffix = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) ".exe" else ""
    val javaHomeCommand = File(System.getProperty("java.home"))
        .resolve("bin")
        .resolve("$command$suffix")
    return if (javaHomeCommand.isFile) javaHomeCommand.absolutePath else command
}

fun generateWindowsInstallerBitmapAssets(outputDir: File): WindowsInstallerBitmapAssets {
    outputDir.mkdirs()
    val classesDir = layout.buildDirectory.dir("tmp/windowsInstallerBitmapGeneratorClasses").get().asFile
    delete(classesDir)
    classesDir.mkdirs()
    runPackageCommand(
        javaExecutable("javac"),
        "-d",
        classesDir.absolutePath,
        rootProject.file("tools/packaging/WindowsInstallerBitmapGenerator.java").absolutePath,
    )
    runPackageCommand(
        javaExecutable("java"),
        "-cp",
        classesDir.absolutePath,
        "org.gem.tools.packaging.WindowsInstallerBitmapGenerator",
        project.file("src/main/package/icons/gem.svg").absolutePath,
        project.file("src/main/package/packaging-visual.properties").absolutePath,
        outputDir.absolutePath,
    )
    val dialogFile = outputDir.resolve("gem-installer-dialog.bmp")
    val bannerFile = outputDir.resolve("gem-installer-banner.bmp")
    require(dialogFile.isFile) { "Installer dialog bitmap was not generated: ${dialogFile.absolutePath}" }
    require(bannerFile.isFile) { "Installer banner bitmap was not generated: ${bannerFile.absolutePath}" }
    return WindowsInstallerBitmapAssets(dialog = dialogFile, banner = bannerFile)
}

tasks.register("generateWindowsInstallerBitmaps") {
    group = "verification"
    description = "Generates the Windows MSI dialog and banner bitmaps from the central package mark."
    doLast {
        val assets = generateWindowsInstallerBitmapAssets(
            layout.buildDirectory.dir("generated/windowsInstallerBitmaps").get().asFile,
        )
        logger.lifecycle("Generated ${assets.dialog.absolutePath}")
        logger.lifecycle("Generated ${assets.banner.absolutePath}")
    }
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

fun shellSingleQuoted(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

fun writeExecutableDebScript(workDir: File, name: String, body: String) {
    val scriptFile = workDir.resolve("DEBIAN/$name")
    scriptFile.parentFile.mkdirs()
    scriptFile.writeText(body.trimIndent() + "\n")
    scriptFile.setExecutable(true, false)
}

fun writeDebLifecyclePreinst(workDir: File) {
    writeExecutableDebScript(
        workDir,
        "preinst",
        """
        #!/bin/sh
        set -eu

        PACKAGE_VERSION=${shellSingleQuoted(desktopPackageVersion)}
        DOWNGRADE_MESSAGE=${shellSingleQuoted(linuxDowngradeMessage)}
        STALE_GEM_MESSAGE=${shellSingleQuoted(linuxStaleGemInstallMessage)}
        STALE_HOSTESS_MESSAGE=${shellSingleQuoted(linuxStaleHostessInstallMessage)}

        installed_version="${'$'}(dpkg-query -W -f='${'$'}{Version}' gema 2>/dev/null || true)"
        if [ -n "${'$'}installed_version" ] && dpkg --compare-versions "${'$'}installed_version" gt "${'$'}PACKAGE_VERSION"; then
            printf '%s\n' "${'$'}DOWNGRADE_MESSAGE" >&2
            exit 1
        fi

        is_project_jpackage_root() {
            root="${'$'}1"
            launcher="${'$'}2"
            cfg="${'$'}3"
            test -x "${'$'}root/bin/${'$'}launcher" && test -f "${'$'}root/lib/app/${'$'}cfg" && test -d "${'$'}root/lib/runtime"
        }

        if is_project_jpackage_root /opt/gem gem gem.cfg; then
            printf '%s\n' "${'$'}STALE_GEM_MESSAGE" >&2
            exit 1
        fi

        if is_project_jpackage_root /opt/hostess hostess hostess.cfg; then
            printf '%s\n' "${'$'}STALE_HOSTESS_MESSAGE" >&2
            exit 1
        fi

        exit 0
        """,
    )
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

fun replaceMacDmgVolumeIcon() {
    if (!System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
        return
    }

    val sourceDmg = dmgArtifact.get().asFile
    require(sourceDmg.isFile) { "Expected DMG artifact missing: ${sourceDmg.absolutePath}" }

    val workDir = layout.buildDirectory.dir("tmp/macDmgVolumeIcon").get().asFile
    delete(workDir)
    workDir.mkdirs()

    val writableDmg = workDir.resolve("gema-${macPackageVersion}-rw.dmg")
    val finalDmg = workDir.resolve("gema-${macPackageVersion}-final.dmg")
    val mountPoint = workDir.resolve("mount")
    mountPoint.mkdirs()

    runPackageCommand(
        "hdiutil",
        "convert",
        sourceDmg.absolutePath,
        "-format",
        "UDRW",
        "-o",
        writableDmg.absolutePath,
    )

    var mounted = false
    try {
        runPackageCommand(
            "hdiutil",
            "attach",
            writableDmg.absolutePath,
            "-mountpoint",
            mountPoint.absolutePath,
            "-nobrowse",
            "-readwrite",
        )
        mounted = true

        project.file("src/main/package/icons/gem.icns")
            .copyTo(mountPoint.resolve(".VolumeIcon.icns"), overwrite = true)
        runPackageCommand("SetFile", "-a", "V", mountPoint.resolve(".VolumeIcon.icns").absolutePath)
        runPackageCommand("SetFile", "-a", "C", mountPoint.absolutePath)
    } finally {
        if (mounted) {
            runPackageCommand("hdiutil", "detach", mountPoint.absolutePath)
        }
    }

    runPackageCommand(
        "hdiutil",
        "convert",
        writableDmg.absolutePath,
        "-format",
        "UDZO",
        "-imagekey",
        "zlib-level=9",
        "-o",
        finalDmg.absolutePath,
    )
    sourceDmg.delete()
    require(finalDmg.renameTo(sourceDmg)) {
        "Unable to replace DMG artifact ${sourceDmg.absolutePath} with icon-normalized image"
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
                packageName = macApplicationBundleName
                packageVersion = macPackageVersion
                dmgPackageVersion = macPackageVersion
                dockName = macApplicationBundleName
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
            writeDebLifecyclePreinst(workDir)

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
            val installerBitmaps = generateWindowsInstallerBitmapAssets(
                layout.buildDirectory.dir("tmp/windowsInstallerBitmaps").get().asFile,
            )
            runPackageCommand(
                "cscript.exe",
                "//NoLogo",
                project.file("src/main/package/windows/patch-msi-display.vbs").absolutePath,
                msiFile.absolutePath,
                windowsDisplayName,
                windowsWelcomeTitle,
                windowsLaunchAfterInstallText,
                project.file("src/main/package/icons/gem.ico").absolutePath,
                installerBitmaps.dialog.absolutePath,
                installerBitmaps.banner.absolutePath,
                windowsDowngradeMessage,
                windowsRunningInstanceMessage,
            )
        }
        "packageDmg" -> doLast {
            normalizeMacDmgArtifact()
            replaceMacDmgVolumeIcon()
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
