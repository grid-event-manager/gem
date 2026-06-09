plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "org.hostess.credential.vault"
        compileSdk = 36
        minSdk = 26
        withHostTestBuilder {}.configure {}
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":hostess-core"))
            }
        }
        val jvmAndroidMain by creating {
            dependsOn(commonMain)
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependsOn(jvmAndroidMain)
        }
        androidMain {
            dependsOn(jvmAndroidMain)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
