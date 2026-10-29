pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "velqor"
include(":app")
include(":core:designsystem")
include(":core:common")
include(":core:filesystem")
include(":core:archive")
include(":core:security")
include(":core:tasks")
include(":feature:browser")
include(":feature:search")
include(":feature:archive")
include(":feature:vault")
include(":feature:editor")
include(":feature:settings")
