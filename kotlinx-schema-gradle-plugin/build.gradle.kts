plugins {
    `dokka-convention`
    `kotlin-jvm-convention`
    `java-gradle-plugin`
    `publishing-convention`
}

gradlePlugin {
    plugins {
        create("kotlinxSchemaPlugin") {
            id = "org.jetbrains.kotlinx.schema.ksp"
            implementationClass = "kotlinx.schema.gradle.KotlinxSchemaPlugin"
            displayName = "Kotlinx Schema Gradle Plugin"
            description = "Gradle plugin for generating JSON schemas using KSP"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation(libs.ksp.gradlePlugin)
    compileOnly(libs.ksp.api)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(gradleTestKit())
}

publishing {
    repositories {
        maven {
            name = "project"
            url = uri(rootProject.layout.buildDirectory.dir("project-repo"))
        }
    }
}

// Fix task dependency issue between dokka and publishing
afterEvaluate {
    tasks.findByName("generateMetadataFileForPluginMavenPublication")?.dependsOn("dokkaJavadocJar")
}
