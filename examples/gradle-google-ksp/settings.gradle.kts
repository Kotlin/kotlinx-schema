pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = uri(rootDir.resolve("../project-repo")))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = uri(rootDir.resolve("../project-repo")))
    }
}
