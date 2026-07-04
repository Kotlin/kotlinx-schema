plugins {
    id("com.vanniktech.maven.publish")
    signing
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "project"
            url = uri(rootProject.layout.buildDirectory.dir("project-repo"))
        }
    }
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

        developers {
            developer {
                id = "JetBrains"
                name = "JetBrains Team"
                organization = "JetBrains"
                organizationUrl = "https://www.jetbrains.com"
            }
        }

        organization {
            name = "JetBrains"
            url = "https://www.jetbrains.com"
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
