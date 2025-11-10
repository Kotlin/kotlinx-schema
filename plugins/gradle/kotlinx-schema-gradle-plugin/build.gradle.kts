import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "kotlinx.schema"
version = "0.1.0"

gradlePlugin {
    plugins {
        create("kotlinxSchemaPlugin") {
            id = "kotlinx.schema"
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

kotlin {
    jvmToolchain(17)
    explicitApi()
    compilerOptions {
        javaParameters = true
        jvmTarget = JvmTarget.JVM_17
    }
}

publishing {
    repositories {
        maven { url = uri(layout.buildDirectory.dir("local-repo")) }
    }
}
