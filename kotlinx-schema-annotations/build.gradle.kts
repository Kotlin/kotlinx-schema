plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    withSourcesJar(publish = true)

    sourceSets {

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
                implementation(kotlin("reflect"))
            }
        }
    }

    explicitApi()
}