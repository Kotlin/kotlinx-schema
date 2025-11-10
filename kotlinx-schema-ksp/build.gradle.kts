plugins {
    kotlin("jvm")
    `dokka-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {

    compilerOptions {
        optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
    }

    dependencies {
        implementation(project(":kotlinx-schema-generator-json"))
        implementation(libs.ksp.api)
        // tests
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(kotlin("reflect"))
    }
}
