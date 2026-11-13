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
        // Facilipay's Maven repository. Pass -PfacilipayMaven=<url or path> to use another one,
        // for example the staging site (https://dev-developers.facilipay.io/maven).
        maven(providers.gradleProperty("facilipayMaven").getOrElse("https://developers.facilipay.io/maven"))
    }
}

rootProject.name = "facilipay-terminal-android"
include(":sample")
