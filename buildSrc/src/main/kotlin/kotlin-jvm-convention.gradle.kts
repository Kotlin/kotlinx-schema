plugins {
    kotlin("jvm")
}

kotlin {

    jvmToolchain(17)

    explicitApi()

    compilerOptions {
        javaParameters = true
        freeCompilerArgs.addAll("-Xdebug")
        optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
    }
}

tasks.test {
    useJUnitPlatform()
}
