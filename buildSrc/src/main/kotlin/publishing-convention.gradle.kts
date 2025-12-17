/**
 * Convention plugin to enable and configure publishing with Maven Publish.
 * - Configures Maven Central publishing with automatic release
 * - Sets up POM metadata for all publications
 * - Enables signing when GPG keys are available
 * - Automatically publishes sources and documentation (KDoc)
 */
plugins {
    id("com.vanniktech.maven.publish")
    signing
    `maven-publish`
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    pom {
        name = providers.gradleProperty("POM_NAME").orElse(project.name).get()
        description =
            providers
                .gradleProperty("POM_DESCRIPTION")
                .orElse(project.description ?: project.name)
                .get()
        url =
            providers
                .gradleProperty("POM_URL")
                .orElse("https://github.com/Kotlin/kotlinx-schema")
                .get()

        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        organization {
            name = "JetBrains"
            url = "https://www.jetbrains.com"
        }

        developers {
            developer {
                id = "kpavlov"
                name = "Konstantin Pavlov"
                email = "k.pavlov@jetbrains.com"
                organization = "JetBrains"
                organizationUrl = "https://jetbrains.com/"
            }
        }

        scm {
            url = "https://github.com/Kotlin/kotlinx-schema"
            connection = "scm:git:https://github.com/Kotlin/kotlinx-schema.git"
            developerConnection = "scm:git:ssh://git@github.com/Kotlin/kotlinx-schema.git"
        }
    }
}

afterEvaluate {
    signing {
        val signingKeyId: String? by project
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        mavenPublishing.signAllPublications()
        isRequired = !signingKey.isNullOrBlank() // don't fail if no key
    }
}
