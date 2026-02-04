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
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.serialization.json)
                implementation(project(":kotlinx-schema-annotations"))
            }
        }

        jvmMain {
            dependencies {
                implementation(kotlin("reflect"))
                runtimeOnly(libs.slf4j.simple)
                compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.junit.jupiter.params)
                implementation(libs.mockk)
                implementation("jakarta.validation:jakarta.validation-api:3.0.2")
            }
        }
    }
}
