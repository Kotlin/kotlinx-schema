import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("plugin.serialization")
    kotlin("jvm") apply true
    alias(libs.plugins.kover) apply false
    `dokka-convention`
    alias(libs.plugins.knit)
}

dependencies {
    implementation(project(":kotlinx-schema-annotations"))
    implementation(project(":kotlinx-schema-generator-json"))
    implementation(libs.kotlinx.serialization.json)

    dokka(project(":kotlinx-schema-annotations"))
    dokka(project(":kotlinx-schema-generator-core"))
    dokka(project(":kotlinx-schema-generator-json"))
    dokka(project(":kotlinx-schema-json"))
    dokka(project(":kotlinx-schema-ksp"))
    dokka(project(":kotlinx-schema-ksp-gradle-plugin"))
}

detekt {
    ignoreFailures = true
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

knit {
    rootDir = project.rootDir
    files =
        fileTree(project.rootDir) {
            include("README.md")
            include("kotlinx-schema-ksp-gradle-plugin/README.md")
            include("kotlinx-schema-json/README.md")
            include("docs/*.md")
        }
    defaultLineSeparator = "\n"
    siteRoot = "https://kotlin.github.io/kotlinx-schema/"
    moduleDocs = "public/apidocs"
}

// Only run knitCheck and knit when explicitly requested, not as part of build/check
afterEvaluate {
    tasks.named("check") {
        if (gradle.startParameter.taskNames.none { it.contains("knit") }) {
            setDependsOn(
                dependsOn.filter {
                    val name =
                        when (it) {
                            is String -> it
                            is Task -> it.name
                            is TaskProvider<*> -> it.name
                            else -> it.toString()
                        }
                    !name.contains("knit")
                },
            )
        }
    }
}

kotlin {
    jvmToolchain(17)
    explicitApi = ExplicitApiMode.Disabled
    compilerOptions {
        allWarningsAsErrors = false
        jvmTarget = JvmTarget.JVM_17
        javaParameters = true
        jvmDefault = JvmDefaultMode.ENABLE
    }

    sourceSets {
        main {
            kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin"))
        }
    }
}
