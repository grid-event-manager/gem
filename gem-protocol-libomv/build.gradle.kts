import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libomvPacketTemplate = layout.projectDirectory.file("src/protocol-bootstrap/message_template.msg")
val generatedLibomvPackets = layout.buildDirectory.dir("generated/sources/libomvPackets/kotlin/commonTest")

val generateLibomvPacketCatalog by tasks.registering {
    group = "build"
    description = "Generates compile-time libomv packet skeletons from the promoted message template."

    inputs.file(libomvPacketTemplate)
    outputs.dir(generatedLibomvPackets)

    doLast {
        val templateFile = libomvPacketTemplate.asFile
        val packetNames = mutableListOf<String>()
        var expectPacketDeclaration = false

        templateFile.forEachLine { rawLine ->
            val line = rawLine.trim()
            when {
                rawLine == "{" -> expectPacketDeclaration = true
                expectPacketDeclaration && (line.isEmpty() || line.startsWith("//")) -> Unit
                expectPacketDeclaration -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 3 && parts[1] in setOf("Fixed", "Low", "Medium", "High")) {
                        packetNames += parts[0]
                    }
                    expectPacketDeclaration = false
                }
            }
        }

        val packetDir = generatedLibomvPackets.get().asFile.resolve("libomv/packets")
        packetDir.deleteRecursively()
        packetDir.mkdirs()

        val templateHash = MessageDigest
            .getInstance("SHA-256")
            .digest(templateFile.readBytes())
            .joinToString(separator = "") { "%02x".format(it) }

        packetDir.resolve("Packet.kt").writeText(
            """
            package libomv.packets

            abstract class Packet {
                abstract val type: PacketType
            }
            """.trimIndent(),
        )

        packetDir.resolve("PacketType.kt").writeText(
            buildString {
                appendLine("package libomv.packets")
                appendLine()
                appendLine("enum class PacketType {")
                appendLine("    Default,")
                packetNames.forEach { appendLine("    $it,") }
                appendLine("}")
            },
        )

        packetDir.resolve("PacketCatalog.kt").writeText(
            buildString {
                appendLine("package libomv.packets")
                appendLine()
                appendLine("object PacketCatalog {")
                appendLine("    const val PACKET_DEFINITION_COUNT: Int = ${packetNames.size}")
                appendLine("    const val GENERATED_KOTLIN_FILE_COUNT: Int = ${packetNames.size + 3}")
                appendLine("    const val MESSAGE_TEMPLATE_SHA256: String = \"$templateHash\"")
                appendLine("    private val PACKET_NAMES: List<String> = listOf(")
                packetNames.forEach { appendLine("        \"$it\",") }
                appendLine("    )")
                appendLine()
                appendLine("    fun packetNames(): List<String> {")
                appendLine("        return PACKET_NAMES.toList()")
                appendLine("    }")
                appendLine("}")
            },
        )

        packetNames.forEach { packetName ->
            packetDir.resolve("${packetName}Packet.kt").writeText(
                """
                package libomv.packets

                class ${packetName}Packet : Packet() {
                    override val type: PacketType = PacketType.$packetName
                }
                """.trimIndent(),
            )
        }
    }
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "org.gem.protocol.libomv"
        compileSdk = 36
        minSdk = 26
        withJava()
        withHostTestBuilder {}.configure {}
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":gem-core"))
            }
        }
        val jvmAndroidMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.okhttp)
            }
        }
        commonTest {
            kotlin.srcDir(generatedLibomvPackets)
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

tasks.matching {
    it.name in setOf(
        "compileTestKotlinJvm",
        "compileCommonTestKotlinMetadata",
        "compileAndroidHostTest",
        "generateAndroidHostTestLintModel",
        "lintAnalyzeAndroidHostTest",
    )
}.configureEach {
    dependsOn(generateLibomvPacketCatalog)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
