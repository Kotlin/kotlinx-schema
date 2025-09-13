package kotlinx.schema.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.File
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaPluginTest {
    @Test
    fun addsCompanionWithJsonSchemaField() {
        val tmpDir = createTempDir(prefix = "kotlin-plugin-test")
        try {
            val srcDir = File(tmpDir, "src").apply { mkdirs() }
            val outDir = File(tmpDir, "out").apply { mkdirs() }

            // Write source file
            val srcFile =
                File(srcDir, "Foo.kt").apply {
                    writeText(
                        """
                        package test

                        import kotlinx.schema.Schema

                        @Schema("{}")
                        class Foo {
                            companion object
                        }
                        """.trimIndent(),
                    )
                }

            // Build classpath from the current test runtime and ensure stdlib/reflect are present
            val cpEntries = System.getProperty("java.class.path").split(File.pathSeparator).toMutableList()
            // Ensure kotlin-stdlib.jar is present
            val stdlibPath = kotlin.Unit::class.java.protectionDomain.codeSource.location.path
            if (stdlibPath != null && stdlibPath.isNotEmpty()) cpEntries += File(stdlibPath).absolutePath
            // Ensure kotlin-reflect.jar is present
            val reflectPath = kotlin.reflect.KClass::class.java.protectionDomain.codeSource.location.path
            if (reflectPath != null && reflectPath.isNotEmpty()) cpEntries += File(reflectPath).absolutePath
            val classpath = cpEntries.joinToString(File.pathSeparator)

            // Package the plugin classes/resources into a temporary JAR and load it via -Xplugin so the compiler can discover the registrar
            val pluginJar = makePluginJar()

            // Compile using the CLI compiler. Our plugin registrar is discovered via META-INF inside the -Xplugin jar.
            val args =
                arrayOf(
                    "-classpath",
                    classpath,
                    "-Xplugin=${pluginJar.absolutePath}",
                    "-d",
                    outDir.absolutePath,
                    srcFile.absolutePath,
                )

            val exit = K2JVMCompiler().exec(System.out, MessageRenderer.PLAIN_RELATIVE_PATHS, *args)
            assertEquals(ExitCode.OK, exit)

            // Load and reflect on the compiled class
            val cl = URLClassLoader(arrayOf(outDir.toURI().toURL()), this::class.java.classLoader)
            val fooClass = cl.loadClass("test.Foo")
            val companionInstance = fooClass.getField("Companion").get(null)
            val companionClass = companionInstance.javaClass
            val field = companionClass.getDeclaredField("jsonSchemaString")
            field.isAccessible = true
            val value = field.get(companionInstance)
            assertEquals("", value)
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}

private fun makePluginJar(): File {
    // Try to locate compiled classes of the plugin
    val codeSource = SchemaCompilerPluginRegistrar::class.java.protectionDomain.codeSource
    val classesPath = codeSource?.location ?: error("Cannot locate plugin classes location")
    val classesFile = File(classesPath.toURI())

    // If it's already a jar, just return it
    if (classesFile.isFile && classesFile.extension.equals("jar", ignoreCase = true)) {
        return classesFile
    }

    // Try to locate resources root that contains META-INF/services
    val serviceRes =
        SchemaPluginTest::class.java.classLoader.getResource(
            "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
        )
    val resourcesRoot: File? =
        serviceRes?.let { url ->
            val resFile = if (url.protocol == "file") File(url.path) else File(url.toURI().path)
            // The resource points to .../build/resources/main/META-INF/services/..., go up 3 levels to reach resources root
            generateSequence(resFile) { it.parentFile }
                .drop(3)
                .firstOrNull()
        }

    val tmpJar = File.createTempFile("schema-plugin", ".jar").apply { deleteOnExit() }
    java.util.zip.ZipOutputStream(tmpJar.outputStream()).use { zos ->
        fun addDir(root: File) {
            if (!root.exists()) return
            root.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel =
                    root
                        .toPath()
                        .relativize(file.toPath())
                        .toString()
                        .replace(File.separatorChar, '/')
                val entry = java.util.zip.ZipEntry(rel)
                zos.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        if (classesFile.isDirectory) addDir(classesFile)
        if (resourcesRoot != null && resourcesRoot.isDirectory) addDir(resourcesRoot)
    }
    return tmpJar
}