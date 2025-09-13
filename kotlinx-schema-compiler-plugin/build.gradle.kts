plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)

    dependencies {
        compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")

        testImplementation(libs.kotlin.test)
        testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
        // Ensure the Schema annotation is on the compiler classpath during tests
        testImplementation(project(":kotlinx-schema-annotations"))
        // Kotlin reflection needed by tooling/tests and to quiet missing warnings
        testImplementation(kotlin("reflect"))
        // Kotlin compile testing
        testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    }

    explicitApi()
}

// Ensure dependencies are visible for JVM project layout as well
dependencies {
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation(kotlin("reflect"))
    testImplementation(project(":kotlinx-schema-annotations"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
}