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
    compilerOptions {
        optIn.set(
            listOf(
                "kotlinx.schema.generator.core.InternalSchemaGeneratorApi",
            ),
        )
    }

    dependencies {
        implementation(project(":kotlinx-schema-generator-json"))
        implementation(libs.ksp.api)
        // tests
        testImplementation(libs.junit.jupiter.params)
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.kotlin.test)
        testImplementation(libs.mockk)
    }
}
