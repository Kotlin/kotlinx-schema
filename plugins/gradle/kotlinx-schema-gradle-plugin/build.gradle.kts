plugins {
    kotlin("jvm") version "2.2.21"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.2")
    compileOnly("com.google.devtools.ksp:symbol-processing-api:2.3.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.21")
    testImplementation("io.kotest:kotest-assertions-core:6.0.4")
    testImplementation(gradleTestKit())
}
