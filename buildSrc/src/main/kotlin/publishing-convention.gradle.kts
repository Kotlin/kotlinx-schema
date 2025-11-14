import com.vanniktech.maven.publish.MavenPublishBaseExtension

/**
 * Convention plugin to enable and configure publishing with Maven Publish.
 * - Configures Maven Central publishing with automatic release
 * - Sets up POM metadata for all publications
 * - Enables signing when GPG keys are available
 * - Automatically publishes sources and documentation (KDoc)
 */
plugins {
    `maven-publish`
    id("com.vanniktech.maven.publish")
    signing
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    configureSigning(this)

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
                id = "kotlin"
                name = "Kotlin Team"
                organization = "JetBrains"
                organizationUrl = "https://kotlinlang.org/"
            }
        }

        scm {
            url = "https://github.com/Kotlin/kotlinx-schema"
            connection = "scm:git:https://github.com/Kotlin/kotlinx-schema.git"
            developerConnection = "scm:git:ssh://git@github.com/Kotlin/kotlinx-schema.git"
        }
    }
}

private fun Project.configureSigning(mavenPublishing: MavenPublishBaseExtension) {
    val gpgKeyName = "GPG_SECRET_KEY"
    val gpgPassphraseName = "SIGNING_PASSPHRASE"
    val signingKey =
        providers
            .environmentVariable(gpgKeyName)
            .orElse(providers.gradleProperty(gpgKeyName))
    val signingPassphrase =
        providers
            .environmentVariable(gpgPassphraseName)
            .orElse(providers.gradleProperty(gpgPassphraseName))

    if (signingKey.isPresent) {
        mavenPublishing.signAllPublications()
        signing.useInMemoryPgpKeys(signingKey.get(), signingPassphrase.orNull)
    }
}
