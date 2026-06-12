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

rootProject.name = "gem"

include(":gem-core")
include(":gem-ui")
include(":gem-credential-vault")
include(":gem-preferences")
include(":gem-protocol-libomv")
include(":tools:cli")
include(":apps:desktop")
include(":apps:android")
