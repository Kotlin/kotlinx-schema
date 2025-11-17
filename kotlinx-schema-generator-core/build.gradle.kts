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
        // production dependencies
        api(project(":kotlinx-schema-annotations"))

        // test dependencies
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.junit.jupiter.params)
        testImplementation(libs.kotest.assertions.json)
    }
}

tasks.test {
    useJUnitPlatform()
}
