plugins {
    kotlin("multiplatform") version "2.3.0"
    kotlin("plugin.serialization") version "2.2.21"
    id("com.google.devtools.ksp") version "2.3.4"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    jvm()

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.koog.agents.tools)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.schema.annotations)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
            }
        }
    }
}

tasks.named("compileKotlinJvm") {
    dependsOn("kspCommonMainKotlinMetadata")
}

tasks.named("compileKotlinJs") {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Configure KSP arguments
ksp {
    arg("kotlinx.schema.withSchemaObject", "true")
    arg("kotlinx.schema.rootPackage", "com.example.shapes")
}

// Add KSP processor for common target
dependencies {
    add("kspCommonMainMetadata", "org.jetbrains.kotlinx:kotlinx-schema-ksp:0.0.2")
}
