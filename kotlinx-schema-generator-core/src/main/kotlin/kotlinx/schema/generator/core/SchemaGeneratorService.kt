package kotlinx.schema.generator.core

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.ServiceLoader
import kotlin.reflect.KClass

public object SchemaGeneratorService {
    private val logger = KotlinLogging.logger {}

    private val serviceLoader = ServiceLoader.load(SchemaGenerator::class.java)
    private var generators: List<SchemaGenerator<*, *>> = serviceLoader.toList()

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
