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
        implementation(project(":kotlinx-schema-generator-json"))
        implementation(libs.ksp.api)
        // tests
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.mockk)
    }
}
