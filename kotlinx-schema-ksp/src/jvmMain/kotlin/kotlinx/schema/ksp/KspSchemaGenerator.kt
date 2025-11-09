package kotlinx.schema.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlinx.schema.generator.core.SchemaGenerator
import kotlinx.schema.generator.core.ir.SchemaEmitter
import kotlinx.schema.generator.core.ir.SchemaIntrospector
import kotlinx.schema.generator.json.internal.IrStandardJsonSchemaEmitter
import kotlinx.schema.ksp.ir.KspIntrospector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * KSP-backed schema generator that uses the generic IR + emitter pipeline.
 * This demonstrates that generators can be generalized to consume abstract metadata
 * (here: KSP symbols) and emit JSON Schema without coupling to KSP or Serialization specifics.
 */
object KspSchemaGenerator : SchemaGenerator<KSClassDeclaration, JsonObject> {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    private val introspector: SchemaIntrospector<KSClassDeclaration> = KspIntrospector()
    private val emitter: SchemaEmitter<JsonObject> = IrStandardJsonSchemaEmitter()

    override fun generateSchema(target: KSClassDeclaration): JsonObject {
        val className = target.simpleName.asString()
        val packageName = target.packageName.asString()
        val qualifiedName = target.qualifiedName?.asString() ?: "$packageName.$className"

        val graph = introspector.introspect(target)
        return emitter.emit(graph, qualifiedName)
    }

    override fun generateSchemaString(target: KSClassDeclaration): String {
        val jsonObject = generateSchema(target)
        return json.encodeToString(jsonObject)
    }
}
