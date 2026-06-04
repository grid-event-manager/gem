import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.jvm)
}

val libomvPacketTemplate = layout.projectDirectory.file("src/protocol-bootstrap/message_template.msg")
val generatedLibomvPackets = layout.buildDirectory.dir("generated/sources/libomvPackets/java/main")

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

        packetDir.resolve("Packet.java").writeText(
            """
            package libomv.packets;

            public abstract class Packet {
                public abstract PacketType getType();
            }
            """.trimIndent(),
        )

        packetDir.resolve("PacketType.java").writeText(
            buildString {
                appendLine("package libomv.packets;")
                appendLine()
                appendLine("public enum PacketType {")
                appendLine("    Default,")
                packetNames.forEach { appendLine("    $it,") }
                appendLine("}")
            },
        )

        packetDir.resolve("PacketCatalog.java").writeText(
            buildString {
                appendLine("package libomv.packets;")
                appendLine()
                appendLine("public final class PacketCatalog {")
                appendLine("    public static final int PACKET_DEFINITION_COUNT = ${packetNames.size};")
                appendLine("    public static final int GENERATED_JAVA_FILE_COUNT = ${packetNames.size + 3};")
                appendLine("    public static final String MESSAGE_TEMPLATE_SHA256 = \"$templateHash\";")
                appendLine("    private static final String[] PACKET_NAMES = new String[] {")
                packetNames.forEach { appendLine("        \"$it\",") }
                appendLine("    };")
                appendLine()
                appendLine("    private PacketCatalog() { }")
                appendLine()
                appendLine("    public static String[] packetNames() {")
                appendLine("        return PACKET_NAMES.clone();")
                appendLine("    }")
                appendLine("}")
            },
        )

        packetNames.forEach { packetName ->
            packetDir.resolve("${packetName}Packet.java").writeText(
                """
                package libomv.packets;

                public final class ${packetName}Packet extends Packet {
                    @Override
                    public PacketType getType() {
                        return PacketType.$packetName;
                    }
                }
                """.trimIndent(),
            )
        }
    }
}

sourceSets {
    named("main") {
        java.srcDir(generatedLibomvPackets)
    }
}

dependencies {
    implementation(project(":hostess-core"))
    testImplementation(kotlin("test"))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateLibomvPacketCatalog)
}

tasks.named("compileKotlin") {
    dependsOn(generateLibomvPacketCatalog)
}

tasks.test {
    useJUnitPlatform()
}
