package kotlinx.schema

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.TYPEALIAS
import kotlin.annotation.AnnotationTarget.TYPE_PARAMETER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * An annotation used to associate a textual description with the target it is applied to.
 *
 * This annotation can be applied to various program elements such as classes, functions,
 * properties, parameters, and more. The description is defined as a `String` value.
 *
 * @property value The description text that provides meta-information about the annotated element.
 */
@Target(
    CLASS,
    ANNOTATION_CLASS,
    TYPE_PARAMETER,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPE,
    FILE,
    TYPEALIAS,
)
@Retention(RUNTIME)
public annotation class Description(
    val value: String,
)

/**
 * Annotation to define a schema for a class or type.
 *
 * This annotation is primarily used to associate a specific schema type with the annotated element.
 * By default, it targets JSON Schema but can accommodate custom schema representations as well.
 *
 * @property value The schema type. Defaults to "json" for JSON Schema.
 * @property withSchemaObject Indicates whether to generate a specific representation for the schema,
 * such as a `JsonObject` for JSON Schema. This may require additional dependencies.
 */
@Target(CLASS)
@Retention(SOURCE)
public annotation class Schema(
    /**
     * Schema Type
     *
     * Default value is "json" for JSON Schema
     */
    val value: String = "json",
    /**
     * Generate specific representation, e.g. JsonObject for JSON Schema.
     *
     * It might require an additional compile-time dependency, e.g. Kotlin Serialization
     */
    val withSchemaObject: Boolean = false,
)
