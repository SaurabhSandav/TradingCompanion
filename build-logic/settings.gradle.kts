
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        google {
            mavenContent {
                includeGroupByRegex(".*google.*")
                includeGroupByRegex(".*android.*")
            }
        }
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":convention")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
