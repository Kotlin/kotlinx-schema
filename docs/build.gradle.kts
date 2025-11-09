plugins {
    kotlin("jvm") apply false
    alias(libs.plugins.kover) apply false
    `dokka-convention`
}

dependencies {
    dokka(project(":kotlinx-schema-annotations"))
    dokka(project(":kotlinx-schema-generator-core"))
    dokka(project(":kotlinx-schema-generator-json"))
    dokka(project(":kotlinx-schema-ksp"))
}

dokka {
    moduleName.set("KotlinX-Schema")

    pluginsConfiguration.html {
        footerMessage = "Copyright © 2025 JetBrains s.r.o."
    }

    dokkaPublications.html {
        outputDirectory = layout.projectDirectory.dir("public/apidocs")
    }
}
