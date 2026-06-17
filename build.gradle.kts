import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}

data class GemBuildVersionState(
    val visibleVersion: String,
    val androidVersionCode: Int,
    val sourceBaseCommit: String,
)

data class GemVersionCommandResult(
    val exitCode: Int,
    val output: String,
)

val gemVersionStatePath = "gradle/gem-version.properties"
val gemVersionStateFile = layout.projectDirectory.file(gemVersionStatePath).asFile
val packageRelevantPathspecs = listOf(
    "apps",
    "gem-core",
    "gem-ui",
    "gem-preferences",
    "gem-credential-vault",
    "gem-protocol-libomv",
    "tools",
    "gradle",
    "build.gradle.kts",
    "settings.gradle.kts",
    ":(exclude)$gemVersionStatePath",
)
val userTestPackageTaskNames = setOf(
    "ratchetGemPackageVersion",
    "packageDeb",
    "packageMsi",
    "packageDmg",
    "packageDistributionForCurrentOS",
    "assembleDebug",
    "assembleRelease",
    "bundleRelease",
)
val userTestPackageBuildRequested = gradle.startParameter.taskNames
    .map { it.substringAfterLast(":") }
    .any { it in userTestPackageTaskNames }

fun runGemVersionCommand(vararg command: String): GemVersionCommandResult {
    val process = ProcessBuilder(*command)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    return GemVersionCommandResult(process.waitFor(), output.trim())
}

fun runGitForGemVersion(vararg arguments: String): GemVersionCommandResult =
    runGemVersionCommand("git", *arguments)

fun requireGitOutputForGemVersion(vararg arguments: String): String {
    val result = runGitForGemVersion(*arguments)
    require(result.exitCode == 0) {
        "Gem version preflight git command failed: git ${arguments.joinToString(" ")}\n${result.output}"
    }
    return result.output
}

fun loadGemBuildVersionState(): GemBuildVersionState {
    require(gemVersionStateFile.isFile) {
        "Gem version state file is missing: ${gemVersionStateFile.absolutePath}"
    }
    val properties = Properties().apply {
        gemVersionStateFile.inputStream().use(::load)
    }
    return GemBuildVersionState(
        visibleVersion = properties.getProperty("visibleVersion")
            ?: error("Gem version state missing visibleVersion"),
        androidVersionCode = properties.getProperty("androidVersionCode")?.toIntOrNull()
            ?: error("Gem version state missing integer androidVersionCode"),
        sourceBaseCommit = properties.getProperty("sourceBaseCommit")
            ?: error("Gem version state missing sourceBaseCommit"),
    )
}

fun writeGemBuildVersionState(state: GemBuildVersionState) {
    gemVersionStateFile.writeText(
        """
        visibleVersion=${state.visibleVersion}
        androidVersionCode=${state.androidVersionCode}
        sourceBaseCommit=${state.sourceBaseCommit}
        """.trimIndent() + "\n",
    )
}

fun patchNumberForGemVersion(visibleVersion: String): Int {
    val parts = visibleVersion.split(".")
    require(parts.size == 3) { "Gem visible version must use major.minor.patch: $visibleVersion" }
    return parts[2].toIntOrNull() ?: error("Gem visible version patch is not an integer: $visibleVersion")
}

fun nextPatchGemVersion(visibleVersion: String): String {
    val parts = visibleVersion.split(".")
    require(parts.size == 3) { "Gem visible version must use major.minor.patch: $visibleVersion" }
    val patch = parts[2].toIntOrNull() ?: error("Gem visible version patch is not an integer: $visibleVersion")
    return "${parts[0]}.${parts[1]}.${patch + 1}"
}

fun macPackageVersionForGemVersion(visibleVersion: String): String =
    "1.0.${patchNumberForGemVersion(visibleVersion)}"

fun assertPackageRelevantTreeCleanForVersionRatchet() {
    val status = runGitForGemVersion(
        "status",
        "--porcelain",
        "--untracked-files=all",
        "--",
        *packageRelevantPathspecs.toTypedArray(),
    )
    require(status.exitCode == 0) {
        "Gem version preflight could not inspect git status:\n${status.output}"
    }
    val dirtyLines = status.output.lines().filter { it.isNotBlank() }
    require(dirtyLines.isEmpty()) {
        """
        User-testable package build has uncommitted package-relevant changes.
        Commit source/build/resource changes before packaging so the automatic version ratchet can anchor the new version to a real source commit.
        Dirty package-relevant paths:
        ${dirtyLines.joinToString("\n")}
        """.trimIndent()
    }
}

fun packageRelevantCodeChangedSince(sourceBaseCommit: String): Boolean {
    val baseExists = runGitForGemVersion("cat-file", "-e", "$sourceBaseCommit^{commit}")
    require(baseExists.exitCode == 0) {
        "Gem version sourceBaseCommit is not present in this git checkout: $sourceBaseCommit"
    }
    val diff = runGitForGemVersion(
        "diff",
        "--quiet",
        sourceBaseCommit,
        "HEAD",
        "--",
        *packageRelevantPathspecs.toTypedArray(),
    )
    return when (diff.exitCode) {
        0 -> false
        1 -> true
        else -> error("Gem version preflight diff failed:\n${diff.output}")
    }
}

fun isGitCheckoutAvailableForVersionRatchet(): Boolean {
    val result = runGitForGemVersion("rev-parse", "--is-inside-work-tree")
    return result.exitCode == 0 && result.output == "true"
}

fun autoRatchetGemBuildVersionIfNeeded(state: GemBuildVersionState): GemBuildVersionState {
    if (!userTestPackageBuildRequested) {
        return state
    }
    if (!isGitCheckoutAvailableForVersionRatchet()) {
        logger.lifecycle(
            "Gem package version ${state.visibleVersion} comes from detached source; " +
                "auto-ratchet requires the primary git checkout.",
        )
        return state
    }
    assertPackageRelevantTreeCleanForVersionRatchet()
    if (!packageRelevantCodeChangedSince(state.sourceBaseCommit)) {
        logger.lifecycle("Gem package version ${state.visibleVersion} is current for package-relevant source.")
        return state
    }
    val updated = state.copy(
        visibleVersion = nextPatchGemVersion(state.visibleVersion),
        androidVersionCode = state.androidVersionCode + 1,
        sourceBaseCommit = requireGitOutputForGemVersion("rev-parse", "HEAD"),
    )
    writeGemBuildVersionState(updated)
    logger.lifecycle(
        "Ratchet Gem package version ${state.visibleVersion} -> ${updated.visibleVersion}; " +
            "Android versionCode ${state.androidVersionCode} -> ${updated.androidVersionCode}.",
    )
    return updated
}

val gemBuildVersionState = autoRatchetGemBuildVersionIfNeeded(loadGemBuildVersionState())
extra["gemVisibleVersion"] = gemBuildVersionState.visibleVersion
extra["gemAndroidVersionCode"] = gemBuildVersionState.androidVersionCode
extra["gemMacPackageVersion"] = macPackageVersionForGemVersion(gemBuildVersionState.visibleVersion)

val checkGemBoundaries by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs Gem public source boundary checks."
    commandLine("bash", layout.projectDirectory.file("tools/guards/check-boundaries.sh").asFile.absolutePath)
}

tasks.register("ratchetGemPackageVersion") {
    group = "build setup"
    description = "Ratchets Gem package version from package-relevant committed source changes."
    doLast {
        logger.lifecycle(
            "Gem package version ${gemBuildVersionState.visibleVersion}; " +
                "Android versionCode ${gemBuildVersionState.androidVersionCode}; " +
                "sourceBaseCommit ${gemBuildVersionState.sourceBaseCommit}.",
        )
    }
}

subprojects {
    group = "org.gem"
    version = "${rootProject.extra["gemVisibleVersion"]}-SNAPSHOT"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }

        tasks.named("check") {
            dependsOn(checkGemBoundaries)
        }
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        tasks.named("check") {
            dependsOn(checkGemBoundaries)
        }
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    plugins.withId("com.android.application") {
        tasks.named("check") {
            dependsOn(checkGemBoundaries)
        }
    }

    plugins.withId("com.android.kotlin.multiplatform.library") {
        tasks.named("check") {
            dependsOn(checkGemBoundaries)
        }
    }
}
