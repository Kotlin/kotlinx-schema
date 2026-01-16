plugins {
    `dokka-convention`
    `kotlin-jvm-convention`
    `java-gradle-plugin`
    `publishing-convention`
}

// https://docs.gradle.org/current/userguide/test_kit.html
val functionalTest: SourceSet = sourceSets.create("functionalTest")

gradlePlugin {
    testSourceSets(functionalTest)
    plugins {
        create("kotlinxSchemaPlugin") {
            id = "org.jetbrains.kotlinx.schema.ksp"
            implementationClass = "kotlinx.schema.ksp.gradle.plugin.KotlinxSchemaPlugin"
            displayName = "Kotlinx Schema Gradle Plugin"
            description = "Generates JSON schemas from Kotlin classes and functions"
            tags =
                listOf(
                    "kotlin",
                    "build",
                    "codegen",
                    "schema",
                    "schema",
                    "jsonschema",
                    "kotlin-multiplatform",
                    "multiplatform",
                    "kmp",
                )
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)

    // KSP2 API for programmatic invocation
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.ksp.symbol.processing.aa.embeddable)
    implementation(libs.ksp.symbol.processing.common.deps)

    testRuntimeOnly(project(":kotlinx-schema-ksp"))
    testRuntimeOnly(project(":kotlinx-schema-annotations"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)

    "functionalTestImplementation"(libs.kotest.assertions.core)
    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"(kotlin("test-junit5"))
    "functionalTestImplementation"(project(":kotlinx-schema-ksp"))
}

tasks.processResources {
    val projectVersion = project.version
    inputs.property("pluginVersion", projectVersion)
    filesMatching("kotlinxSchema.properties") {
        expand("pluginVersion" to projectVersion)
    }
}

tasks.validatePlugins {
    enableStricterValidation = true
}

// Fix task dependency issue between dokka and publishing
afterEvaluate {
    tasks.findByName("generateMetadataFileForPluginMavenPublication")?.dependsOn("dokkaJavadocJar")
}

val functionalTestTask =
    tasks.register<Test>("functionalTest") {
        group = "verification"
        testClassesDirs = functionalTest.output.classesDirs
        classpath = functionalTest.runtimeClasspath
        useJUnitPlatform()
    }
