import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

kotlin {

    jvmToolchain(17)

    explicitApi()

    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        javaParameters = true
        freeCompilerArgs.addAll("-Xdebug")
    }
}

tasks.test {
    useJUnitPlatform()
}
