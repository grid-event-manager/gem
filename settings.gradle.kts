pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "hostess"

include(":hostess-core")
include(":hostess-ui")
include(":hostess-credential-vault")
include(":hostess-protocol-libomv")
include(":tools:cli")
include(":apps:desktop")
include(":apps:android")
