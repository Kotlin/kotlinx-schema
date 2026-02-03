import org.gradle.kotlin.dsl.invoke

plugins {
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
                implementation(libs.kotlin.logging)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":kotlinx-schema-annotations"))
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)

                implementation(libs.kotlinx.serialization.json)
            }
        }

        jvmMain {
            dependencies {
                implementation(kotlin("reflect"))
                runtimeOnly(libs.slf4j.simple)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.junit.jupiter.params)
                implementation(libs.mockk)
            }
        }
    }
}
