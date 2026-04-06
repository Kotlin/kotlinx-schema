package kotlinx.schema

/**
 * An annotation used to associate a textual description with the target it is applied to.
 *
 * This annotation can be applied to various program elements such as classes, functions,
 * properties, parameters, and more. The description is defined as a `String` value.
 *
 * @property value The description text that provides meta-information about the annotated element.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FILE,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class Description(
    val value: String,
)
