
pluginManagement {

    repositories {
        google {
            mavenContent {
                includeGroupByRegex(".*google.*")
                includeGroupByRegex(".*android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("PropertyName")
val GITHUB_ACTOR: String? by settings

@Suppress("PropertyName")
val GITHUB_TOKEN: String? by settings

dependencyResolutionManagement {

    @Suppress("UnstableApiUsage")
    repositories {
        google {
            mavenContent {
                includeGroupByRegex(".*google.*")
                includeGroupByRegex(".*android.*")
            }
        }
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/saurabhsandav/CommonVersions")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: GITHUB_ACTOR
                password = System.getenv("GITHUB_TOKEN") ?: GITHUB_TOKEN
            }
        }
    }

    versionCatalogs {

        create("libs") {
            from("com.saurabhsandav:common-versions:0.99.0")
        }
    }
}

rootProject.name = "TradingCompanion"

include(
    "app",
    "fyers-api",
    "lightweight-charts",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
