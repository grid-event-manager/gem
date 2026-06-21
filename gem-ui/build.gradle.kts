import org.gem.build.localization.GemLocalizationGeneratorTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

val materialIconsCore = libs.compose.material.icons.core
val generateGemLocalization by tasks.registering(GemLocalizationGeneratorTask::class) {
    sourceDirectory.set(layout.projectDirectory.dir("src/commonMain/localization"))
    generatedSourceDirectory.set(layout.buildDirectory.dir("generated/source/gemLocalization/commonMain/kotlin"))
    reportDirectory.set(layout.buildDirectory.dir("reports/gem-localization"))
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "org.gem.ui"
        compileSdk = 36
        minSdk = 26
        withHostTestBuilder {}.configure {}
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateGemLocalization.flatMap { it.generatedSourceDirectory })

            dependencies {
                implementation(project(":gem-core"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(materialIconsCore)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateGemLocalization)
}
