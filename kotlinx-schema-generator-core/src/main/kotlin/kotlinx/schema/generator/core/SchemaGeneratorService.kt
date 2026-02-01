package kotlinx.schema.generator.core

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 * Service object responsible for discovering and managing schema generators.
 *
 * This service uses a `ServiceLoader` mechanism to dynamically discover all implementations
 * of the `SchemaGenerator` interface. It provides access to the list of available schema
 * generators and allows querying for a specific generator based on its target and schema
 * types.
 */
public object SchemaGeneratorService {
    private val logger = KotlinLogging.logger {}

    private val serviceLoader = ServiceLoader.load(SchemaGenerator::class.java)
    private val generators: List<SchemaGenerator<*, *>> = serviceLoader.toList()

    init {
        logger.info {
            "Discovered SchemaGenerators: ${serviceLoader.map { it::class.qualifiedName }}"
        }
    }

    public fun registeredGenerators(): Collection<SchemaGenerator<*, *>> = generators

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any, R : Any> getGenerator(
        targetType: KClass<T>? = null,
        schemaType: KClass<R>? = null,
    ): SchemaGenerator<T, R>? =
        generators
            .filter { targetType == null || it.targetType() == targetType }
            .singleOrNull { schemaType == null || it.schemaType() == schemaType } as? SchemaGenerator<T, R>
}
