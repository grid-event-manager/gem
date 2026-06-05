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
        namespace = "org.hostess.core"
        compileSdk = 36
        minSdk = 26
        withHostTestBuilder {}.configure {}
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            // HS001-F-08 deletion gate: temporary old source root until core files move to commonMain.
            kotlin.srcDir("src/main/kotlin")
        }
        androidMain {
            // HS001-F-08 deletion gate: temporary old source root until core files move to commonMain.
            kotlin.srcDir("src/main/kotlin")
        }
        jvmTest {
            // HS001-F-08 deletion gate: temporary old test root until core tests move to commonTest.
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
