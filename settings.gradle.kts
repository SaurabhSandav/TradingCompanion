
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
    }
}

rootProject.name = "TradingCompanion"

include(
    "app",
    "fyers-api",
    "lightweight-charts",
    "jcef-compose",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
