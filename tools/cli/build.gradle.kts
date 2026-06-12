plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":gem-core"))
    implementation(project(":gem-protocol-libomv"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.gem.tools.cli.GemCli")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
