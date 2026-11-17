pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "IlCamminoDiFrancesco"

include(":app")
include(":core:model")
include(":core:content")
include(":core:narrative")
include(":core:progression")
include(":core:nostr")
include(":core:database")
include(":core:designsystem")
