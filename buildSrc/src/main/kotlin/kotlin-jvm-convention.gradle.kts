import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

kotlin {

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        // Use the set() function to ensure compatibility with older Gradle versions
        enabled.set(true)
    }

    jvmToolchain(17)

    explicitApi()

    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        javaParameters = true
        jvmDefault = JvmDefaultMode.ENABLE
        freeCompilerArgs.addAll(
            "-Xdebug",
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
