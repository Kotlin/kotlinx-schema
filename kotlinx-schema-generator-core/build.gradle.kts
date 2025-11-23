plugins {
    `dokka-convention`
    `kotlin-jvm-convention`
    `publishing-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {
    dependencies {
        implementation(libs.kotlin.logging)
        implementation(kotlin("reflect"))
        runtimeOnly(libs.slf4j.simple)

        // test dependencies
        testImplementation(project(":kotlinx-schema-annotations"))
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.junit.jupiter.params)
        testImplementation(libs.kotest.assertions.json)
        testImplementation(libs.mockk)
        testImplementation(libs.kotlinx.serialization.json)
    }
}
