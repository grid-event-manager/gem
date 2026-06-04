plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

val checkHostessBoundaries by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs Hostess public source boundary checks."
    commandLine("bash", layout.projectDirectory.file("tools/guards/check-boundaries.sh").asFile.absolutePath)
}

subprojects {
    group = "org.hostess"
    version = "0.1.0-SNAPSHOT"

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

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
