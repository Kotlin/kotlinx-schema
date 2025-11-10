plugins {
    kotlin("jvm")
}

kotlin {

    jvmToolchain(17)

    explicitApi()

    compilerOptions {
        javaParameters = true
        optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
    }
}
