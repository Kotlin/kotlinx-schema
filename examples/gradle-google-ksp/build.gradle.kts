plugins {
    kotlin("multiplatform") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.google.ksp)
}

val kotlinxSchemaVersion = project.properties["kotlinxSchemaVersion"]

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    jvm()

    js {
        nodejs()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.koog.agents.tools)
                implementation(libs.kotlinx.serialization.json)
                implementation("org.jetbrains.kotlinx:kotlinx-schema-annotations:$kotlinxSchemaVersion")
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
    arg("kotlinx.schema.visibility", "internal")
}

// Add KSP processor for common target
dependencies {
    add("kspCommonMainMetadata", "org.jetbrains.kotlinx:kotlinx-schema-ksp:$kotlinxSchemaVersion")
}
