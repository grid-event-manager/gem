plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}

val checkHostessBoundaries by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs Hostess public source boundary checks."
    commandLine("bash", layout.projectDirectory.file("tools/guards/check-boundaries.sh").asFile.absolutePath)
}

subprojects {
    group = "org.hostess"
version = "0.1.10-SNAPSHOT"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }

        tasks.named("check") {
            dependsOn(checkHostessBoundaries)
        }
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        tasks.named("check") {
            dependsOn(checkHostessBoundaries)
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
            dependsOn(checkHostessBoundaries)
        }
    }

    plugins.withId("com.android.kotlin.multiplatform.library") {
        tasks.named("check") {
            dependsOn(checkHostessBoundaries)
        }
    }
}
