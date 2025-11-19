plugins {
    kotlin("multiplatform")
}

kotlin {

    compilerOptions {
        allWarningsAsErrors = true
        extraWarnings = true
        freeCompilerArgs =
            listOf(
                "-Wextra",
                "-Xjvm-default=all",
                "-Xmulti-dollar-interpolation",
            )
    }

    withSourcesJar(publish = true)
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    explicitApi()

    jvm {
        //    jvmToolchain(17)
//        compilerOptions {
//            javaParameters = true
//            freeCompilerArgs.addAll("-Xdebug")
//            optIn.set(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
//        }

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        browser()
        nodejs()
    }
    wasmJs {
        binaries.library()
        browser {}
        nodejs()
    }

    macosArm64()
    iosArm64()
    iosSimulatorArm64()
}
