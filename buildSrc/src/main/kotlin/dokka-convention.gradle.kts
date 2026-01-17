/**
 * Common conventions for generating documentation with Dokka.
 */
plugins {
    id("org.jetbrains.dokka") apply true
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
        sourceLink {
            // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
            remoteUrl("https://github.com/Kotlin/kotlinx-schema/blob/main")
            localDirectory.set(rootDir)
        }
        externalDocumentationLinks.register("ktor-client") {
            url("https://api.ktor.io/ktor-client/")
            packageListUrl("https://api.ktor.io/package-list")
        }
        externalDocumentationLinks.register("kotlinx-coroutines") {
            url("https://kotlinlang.org/api/kotlinx.coroutines/")
            packageListUrl("https://kotlinlang.org/api/kotlinx.coroutines/package-list")
        }
        externalDocumentationLinks.register("kotlinx-serialization") {
            url("https://kotlinlang.org/api/kotlinx.serialization/")
            packageListUrl("https://kotlinlang.org/api/kotlinx.serialization/package-list")
        }
    }
}
