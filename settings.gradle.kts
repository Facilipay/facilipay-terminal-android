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
        // Facilipay's Maven repository, the only one allowed to serve io.facilipay. Pass
        // -PfacilipayMaven=<url or path> to use another one, for example the staging site
        // (https://dev-developers.facilipay.io/maven) or a local build of the library.
        exclusiveContent {
            forRepository {
                maven(providers.gradleProperty("facilipayMaven").getOrElse("https://developers.facilipay.io/maven"))
            }
            filter { includeGroup("io.facilipay") }
        }
    }
}

rootProject.name = "facilipay-terminal-android"
include(":terminal-sdk", ":sample")
