package kotlinx.schema.ksp

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlinx.schema.generator.core.AbstractSchemaGenerator
import kotlinx.schema.generator.json.TypeGraphToFunctionCallingSchemaTransformer
import kotlinx.schema.json.FunctionCallingSchema
import kotlinx.schema.ksp.ir.KspFunctionIntrospector
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * A concrete implementation of [AbstractSchemaGenerator] that generates FunctionCallingSchema
 * for Kotlin functions using the Kotlin Symbol Processing (KSP) API.
 *
 * This generator uses [KspFunctionIntrospector] to analyze Kotlin functions and produce a type graph,
 * and [TypeGraphToFunctionCallingSchemaTransformer] to generate a FunctionCallingSchema representation
 * from the type graph. The schema is serialized into a JSON string using Kotlinx Serialization.
 *
 * This generator supports:
 * - Regular functions
 * - Suspend functions (treated as regular functions)
 * - Extension functions
 * - Functions with default parameters
 * - Functions with complex parameter types
 */
internal class KspFunctionSchemaGenerator :
    AbstractSchemaGenerator<KSFunctionDeclaration, FunctionCallingSchema>(
        introspector = KspFunctionIntrospector(),
        typeGraphTransformer = TypeGraphToFunctionCallingSchemaTransformer(),
    ) {
    private val json =
        Json {
            prettyPrint = false
            encodeDefaults = false
        }

    override fun getRootName(target: KSFunctionDeclaration): String {
        val functionName = target.simpleName.asString()
        val packageName = target.packageName.asString()
        return target.qualifiedName?.asString() ?: "$packageName.$functionName"
    }

    override fun targetType(): KClass<KSFunctionDeclaration> = KSFunctionDeclaration::class

    override fun schemaType(): KClass<FunctionCallingSchema> = FunctionCallingSchema::class

    override fun encodeToString(schema: FunctionCallingSchema): String = json.encodeToString(schema)
}
