plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":hostess-core"))
    implementation(project(":hostess-credential-vault"))
    implementation(project(":hostess-protocol-libomv"))
    testImplementation(kotlin("test-junit"))
}
