
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

    versionCatalogs {

        create("libs") {
            from("com.saurabhsandav:common-versions:0.84.0")
            version("kotlin", "1.9.20-RC")
            version("jetbrainsCompose", "1.5.10-rc01")
        }
    }
}

rootProject.name = "TradingCompanion"
