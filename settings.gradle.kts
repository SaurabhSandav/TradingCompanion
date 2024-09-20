
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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
            from("com.saurabhsandav:common-versions:0.97.0")
            version("jcefMaven", "122.1.10")
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
