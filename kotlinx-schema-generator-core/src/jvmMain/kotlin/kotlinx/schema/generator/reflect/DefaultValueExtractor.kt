package kotlinx.schema.generator.reflect

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1

/**
 * Extracts default values from data class properties using reflection.
 *
 * It works by attempting to create an instance of the class using its primary constructor,
 * providing "mock" values for required parameters and letting Kotlin fill in the defaults
 * for optional parameters.
 */
internal object DefaultValueExtractor {
    private val cache = ConcurrentHashMap<KClass<*>, Map<String, Any?>>()

    /**
     * Extracts default values for the given [KClass].
     * Results are cached for performance.
     */
    fun extractDefaultValues(klass: KClass<*>): Map<String, Any?> =
        cache.getOrPut(klass) {
            doExtractDefaultValues(klass)
        }

    private fun KClass<*>.extractInstanceProperties(instance: Any): Map<String, Any?> =
        this.members
            .filterIsInstance<KProperty1<Any, *>>()
            .associate { prop ->
                prop.name to
                    try {
                        prop.get(instance)
                    } catch (_: Exception) {
                        null
                    }
            }.filterValues { it != null }

    @Suppress("ReturnCount")
    private fun doExtractDefaultValues(klass: KClass<*>): Map<String, Any?> {
        // For object instances (singletons), get the instance directly
        klass.objectInstance?.let { return klass.extractInstanceProperties(it) }

        val constructor = findPrimaryConstructor(klass) ?: return emptyMap()

        val requiredParams = constructor.parameters.filter { !it.isOptional }

        // Build map of required parameters with mock values
        val paramMap = mutableMapOf<KParameter, Any?>()
        for (param in requiredParams) {
            if (param.type.isMarkedNullable) {
                paramMap[param] = null
            } else {
                getDefaultValueForParameter(param)?.let { defaultValue ->
                    paramMap[param] = defaultValue
                }
            }
        }

        // Create an instance with only required parameters to get defaults
        val instance =
            try {
                constructor.callBy(paramMap)
            } catch (_: Exception) {
                return emptyMap()
            }

        // Extract property values from the instance
        return klass.extractInstanceProperties(instance)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getDefaultValueForParameter(param: KParameter): Any? {
        val classifier = param.type.classifier
        return when {
            param.type.isMarkedNullable -> null

            classifier == String::class -> ""

            classifier == Int::class -> 0

            classifier == Long::class -> 0L

            classifier == Double::class -> 0.0

            classifier == Float::class -> 0.0f

            classifier == Boolean::class -> false

            classifier is KClass<*> && classifier.java.isEnum -> classifier.java.enumConstants.firstOrNull()

            classifier is KClass<*> && Set::class.java.isAssignableFrom(classifier.java) -> emptySet<Any>()

            classifier is KClass<*> &&
                Iterable::class.java.isAssignableFrom(
                    classifier.java,
                )
            -> emptyList<Any>()

            classifier is KClass<*> &&
                Map::class.java.isAssignableFrom(
                    classifier.java,
                )
            -> emptyMap<Any, Any>()

            else -> null // Can't provide mock value for an unknown non-nullable type
        }
    }
}
