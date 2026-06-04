plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":hostess-core"))
    implementation(project(":hostess-protocol-libomv"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.hostess.tools.cli.HostessCli")
}

tasks.test {
    useJUnitPlatform()
}
