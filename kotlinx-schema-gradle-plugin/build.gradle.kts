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
            implementationClass = "kotlinx.schema.gradle.KotlinxSchemaPlugin"
            displayName = "Kotlinx Schema Gradle Plugin"
            description = "Gradle plugin for generating JSON schemas using KSP"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")

    // KSP2 API for programmatic invocation
    implementation("com.google.devtools.ksp:symbol-processing-api:${libs.versions.ksp.get()}")
    implementation("com.google.devtools.ksp:symbol-processing-aa-embeddable:${libs.versions.ksp.get()}")
    implementation("com.google.devtools.ksp:symbol-processing-common-deps:${libs.versions.ksp.get()}")

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

val functionalTestTask =
    tasks.register<Test>("functionalTest") {
        group = "verification"
        testClassesDirs = functionalTest.output.classesDirs
        classpath = functionalTest.runtimeClasspath
        useJUnitPlatform()
    }
