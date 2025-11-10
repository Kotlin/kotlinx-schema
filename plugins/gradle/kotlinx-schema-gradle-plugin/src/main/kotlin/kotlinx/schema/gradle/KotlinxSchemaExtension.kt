package kotlinx.schema.gradle

import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring the Kotlinx Schema Gradle plugin.
 *
 * Example usage:
 * ```kotlin
 * kotlinxSchema {
 *     enabled.set(true)
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

        init {
            // Set default values
            enabled.convention(true)
            // rootPackage intentionally has no default; absence means no filtering
        }
    }
