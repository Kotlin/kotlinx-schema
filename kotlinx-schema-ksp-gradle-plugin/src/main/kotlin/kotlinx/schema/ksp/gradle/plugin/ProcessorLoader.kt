package kotlinx.schema.ksp.gradle.plugin

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import org.gradle.api.file.ConfigurableFileCollection
import java.net.URLClassLoader
import java.util.ServiceLoader

/**
 * Loads KSP processors from the given classpath.
 *
 * @param processorClasspath The classpath containing processor JARs
 * @return List of loaded processor providers, or empty list if none found
 */
internal fun loadProcessors(processorClasspath: ConfigurableFileCollection): List<SymbolProcessorProvider> {
    if (processorClasspath.isEmpty) return emptyList()

    val urls = processorClasspath.files.map { it.toURI().toURL() }.toTypedArray()
    val classLoader = URLClassLoader(urls, SymbolProcessorProvider::class.java.classLoader)

    return ServiceLoader
        .load(SymbolProcessorProvider::class.java, classLoader)
        .toList()
}
