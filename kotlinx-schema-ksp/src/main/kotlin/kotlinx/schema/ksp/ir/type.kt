package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import kotlinx.schema.generator.core.ir.TypeId

/**
 * Generates a stable identifier for a type definition.
 *
 * The method constructs a `TypeId` for the given `KSClassDeclaration`, which is used
 * for deduplication and $ref linking in type schemas. If the class has a qualified name,
 * it is used; otherwise, the simple name is used as a fallback.
 *
 * @return A `TypeId` representing the identifier of the type.
 */
internal fun KSClassDeclaration.typeId(): TypeId = TypeId(qualifiedName?.asString() ?: simpleName.asString())

/**
 * Checks if a KSType represents a sealed class and returns the declaration if so.
 *
 * Eliminates repeated casting pattern: `type.declaration is KSClassDeclaration &&
 * (type.declaration as KSClassDeclaration).modifiers.contains(Modifier.SEALED)`
 *
 * @return KSClassDeclaration if this is a sealed class, null otherwise
 */
internal fun KSType.sealedClassDeclOrNull(): KSClassDeclaration? {
    val decl = declaration as? KSClassDeclaration ?: return null
    return if (decl.modifiers.contains(Modifier.SEALED)) decl else null
}

/**
 * Checks if a KSType represents an enum class and returns the declaration if so.
 *
 * Eliminates repeated casting pattern: `type.declaration is KSClassDeclaration &&
 * (type.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS`
 *
 * @return KSClassDeclaration if this is an enum class, null otherwise
 */
internal fun KSType.enumClassDeclOrNull(): KSClassDeclaration? {
    val decl = declaration as? KSClassDeclaration ?: return null
    return if (decl.classKind == ClassKind.ENUM_CLASS) decl else null
}
