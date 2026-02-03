plugins {
    kotlin("plugin.serialization")
    `dokka-convention`
    `kotlin-multiplatform-convention`
    `publishing-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
                api(project(":kotlinx-schema-annotations"))
                api(project(":kotlinx-schema-generator-core"))
                api(project(":kotlinx-schema-json"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
                implementation(libs.kotlin.test)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.junit.pioneer)
                implementation(libs.mockk)
            }
        }
    }

    compilerOptions {
        optIn.set(
            listOf(
                "kotlinx.serialization.ExperimentalSerializationApi",
            ),
        )
    }
}
