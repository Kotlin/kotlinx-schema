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

        externalDocumentationLinks.register("kotlinx-schema") {
            url("https://kotlin.github.io/kotlinx-schema/")
            packageListUrl("https://kotlin.github.io/kotlinx-schema/package-list")
        }

        externalDocumentationLinks.register("ktor") {
            url("https://api.ktor.io")
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
