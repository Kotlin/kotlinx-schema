pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = uri(rootDir.resolve("../../build/project-repo")))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = uri(rootDir.resolve("../../build/project-repo")))
    }
}
