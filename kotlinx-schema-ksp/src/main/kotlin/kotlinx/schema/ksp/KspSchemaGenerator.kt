package kotlinx.schema.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.json.internal.IrStandardJsonSchemaEmitter
import kotlinx.schema.ksp.ir.KspIntrospector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass

/**
 * A concrete implementation of [AbstractSchemaGenerator] that generates JSON schema representations
 * for Kotlin classes using the Kotlin Symbol Processing (KSP) API.
 *
 * This object uses [KspIntrospector] to analyze Kotlin classes and produce a type graph, and
 * [IrStandardJsonSchemaEmitter] to generate a JSON schema representation from the type graph. The schema
 * is serialized into a JSON string using Kotlinx Serialization.
 *
 * This generator is designed to support producing schemas with:
 * - Pretty-printed JSON
 * - Default values included in the output
 */
internal class KspSchemaGenerator :
    AbstractSchemaGenerator<KSClassDeclaration, JsonObject>(
        introspector = KspIntrospector(),
        emitter = IrStandardJsonSchemaEmitter(),
    ) {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    override fun getRootName(target: KSClassDeclaration): String {
        val className = target.simpleName.asString()
        val packageName = target.packageName.asString()
        return target.qualifiedName?.asString() ?: "$packageName.$className"
    }

    override fun targetType(): KClass<KSClassDeclaration> = KSClassDeclaration::class

    override fun schemaType(): KClass<JsonObject> = JsonObject::class

    override fun encodeToString(schema: JsonObject): String = json.encodeToString(schema)
}
