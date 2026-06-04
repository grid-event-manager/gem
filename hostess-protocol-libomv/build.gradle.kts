plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":hostess-core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
