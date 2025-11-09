plugins {
    alias(libs.plugins.kotlinMultiplatform)
    `dokka-convention`
}

dokka {
    dokkaSourceSets.configureEach {
    }
}

kotlin {
    jvm {
        compilerOptions {
            optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
        }
    }
    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":kotlinx-schema-generator-json"))
                implementation(libs.ksp.api)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
                implementation(kotlin("reflect"))
            }
        }
    }
}
