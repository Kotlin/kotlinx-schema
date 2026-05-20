import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
}

kotlin {

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }

    jvmToolchain(17)

    explicitApi()

    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        javaParameters = true
        jvmDefault = JvmDefaultMode.ENABLE
        freeCompilerArgs.addAll("-Xdebug")
    }
}

tasks.test {
    useJUnitPlatform()
}
