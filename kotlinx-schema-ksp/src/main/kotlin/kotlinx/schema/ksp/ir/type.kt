package kotlinx.schema.ksp.ir

import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlinx.schema.generator.core.ir.TypeId

internal fun KSClassDeclaration.typeId(): TypeId = TypeId(qualifiedName?.asString() ?: simpleName.asString())
