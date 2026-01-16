package kotlinx.schema.ksp.gradle.plugin

import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring the Kotlinx Schema Gradle plugin.
 *
 * Example usage:
 * ```kotlin
 * kotlinxSchema {
 *     enabled.set(true)
 *     rootPackage.set("com.example.models")
 *     withSchemaObject.set(true)
 * }
 * ```
 */
public abstract class KotlinxSchemaExtension
    @Inject
    constructor() {
        /**
         * Whether schema generation is enabled.
         * Default: true
         */
        public abstract val enabled: Property<Boolean>

        /**
         * Optional root package to limit processing to. If set, only classes in this package
         * (or its subpackages) will be processed by KSP. If not set, all packages are processed.
         */
        public abstract val rootPackage: Property<String>

        /**
         * Whether to generate the jsonSchema JsonObject property in addition to jsonSchemaString.
         * When enabled, both extension properties are generated:
         * - jsonSchemaString: String
         * - jsonSchema: JsonObject
         *
         * Default: true
         */
        public abstract val withSchemaObject: Property<Boolean>

        init {
            // Set default values
            enabled.convention(true)
            withSchemaObject.convention(false) // Enable JsonObject property by default
            rootPackage.unsetConvention() // rootPackage intentionally has no default; absence means no filtering
        }
    }
