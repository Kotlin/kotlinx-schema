import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.schema.ksp")
}

kotlinxSchema {
    enabled.set(true)
    rootPackage.set("kotlinx.schema.jvm")
    withSchemaObject.set(true)
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_1
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

tasks.test {
    useJUnitPlatform()
}

val kotlinxSchemaVersion = project.properties["kotlinxSchemaVersion"]

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(
        "org.jetbrains.kotlinx:kotlinx-schema-annotations:$kotlinxSchemaVersion",
    )

    testImplementation(kotlin("test-junit5"))
}
