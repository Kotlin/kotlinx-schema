plugins {
    `dokka-convention`
    `kotlin-jvm-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {

    dependencies {
        implementation(project(":kotlinx-schema-generator-json"))
        implementation(libs.ksp.api)
        // tests
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(kotlin("reflect"))
    }
}
