@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package kotlinx.schema.compiler

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(UnsafeDuringIrConstructionAPI::class, ObsoleteDescriptorBasedAPI::class)
public class SchemaIrGenerationExtension : IrGenerationExtension {
    private val schemaAnnotationFqName = FqName("kotlinx.schema.Schema")

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        moduleFragment.files.forEach { file ->
            file.declarations.filterIsInstance<IrClass>().forEach { irClass ->
                processClass(irClass, pluginContext)
            }
        }
    }

    private fun processClass(
        irClass: IrClass,
        pluginContext: IrPluginContext,
    ) {
        // Recurse into inner classes as well
        irClass.declarations.filterIsInstance<IrClass>().forEach { processClass(it, pluginContext) }

        val ann = irClass.annotations.findAnnotation(schemaAnnotationFqName) ?: return

        val companion =
            irClass.declarations.filterIsInstance<IrClass>().firstOrNull { it.isCompanion } ?: createCompanion(
                irClass,
                pluginContext,
            )
        ensureJsonSchemaField(companion, pluginContext)
    }

    @OptIn(DeprecatedForRemovalCompilerApi::class)
    private fun createCompanion(
        irClass: IrClass,
        pluginContext: IrPluginContext,
    ): IrClass {
        val irFactory = pluginContext.irFactory
        val companion =
            irFactory
                .buildClass {
                    name = Name.identifier("Companion")
                    kind = ClassKind.OBJECT
                    visibility = DescriptorVisibilities.PUBLIC
                    modality = Modality.FINAL
                    isCompanion = true
                }.apply {
                    parent = irClass
                    superTypes = listOf(pluginContext.irBuiltIns.anyType)
                    // Initialize implicit this receiver for the class (required for defaultType)
                    this.createImplicitParameterDeclarationWithWrappedDescriptor()
                }
        irClass.declarations.add(companion)
        return companion
    }

    private fun ensureJsonSchemaField(
        companion: IrClass,
        pluginContext: IrPluginContext,
    ) {
        val existingField =
            companion.declarations.filterIsInstance<IrField>().firstOrNull { it.name.asString() == "jsonSchemaString" }
        if (existingField != null) return

        val stringType = pluginContext.irBuiltIns.stringType
        val irFactory = pluginContext.irFactory

        val field =
            irFactory
                .buildField {
                    name = Name.identifier("jsonSchemaString")
                    type = stringType
                    visibility = DescriptorVisibilities.PUBLIC
                    isFinal = true
                    isStatic = false // instance field of the object
                    origin = IrDeclarationOrigin.DEFINED
                }.apply {
                    parent = companion
                    initializer =
                        pluginContext.irFactory.createExpressionBody(
                            IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, stringType, ""),
                        )
                }

        companion.declarations.add(field)
    }
}