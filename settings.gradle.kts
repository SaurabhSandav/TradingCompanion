pluginManagement {

    repositories {
        google()
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
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://androidx.dev/storage/compose-compiler/repository/")
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/saurabhsandav/CommonVersions")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: GITHUB_ACTOR
                password = System.getenv("GITHUB_TOKEN") ?: GITHUB_TOKEN
            }
        }
    }

    @Suppress("UnstableApiUsage")
    versionCatalogs {

        create("libs") {
            from("com.saurabhsandav:common-versions:0.73.0")
            version("jetbrainsCompose", "1.3.0")
        }
    }
}

rootProject.name = "TradingCompanion"
