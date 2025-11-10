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
        // production dependencies
        api(project(":kotlinx-schema-annotations"))

        // test dependencies
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.kotest.assertions.json)
    }
}
