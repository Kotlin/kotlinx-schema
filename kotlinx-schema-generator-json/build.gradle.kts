plugins {
    kotlin("plugin.serialization") apply true
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
        api(libs.kotlinx.serialization.json)
        api(project(":kotlinx-schema-annotations"))
        api(project(":kotlinx-schema-generator-core"))
        api(project(":kotlinx-schema-json"))

        // test dependencies
        testImplementation(libs.junit.pioneer)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.kotest.assertions.json)
        testImplementation(libs.kotlin.test)
    }

    compilerOptions {
        optIn.set(
            listOf(
                "kotlinx.serialization.ExperimentalSerializationApi",
            ),
        )
    }
}
