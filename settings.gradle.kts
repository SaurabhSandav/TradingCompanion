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
        mavenLocal()
    }
}

rootProject.name = "TradingCompanion"

include(
    "app",
    "fyers-api",
    "lightweight-charts",
    "jcef-compose",
    "paging",
    "trading:core",
    "trading:indicator",
    "trading:barreplay",
    "trading:record",
    "trading:backtest",
    "trading:test",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
