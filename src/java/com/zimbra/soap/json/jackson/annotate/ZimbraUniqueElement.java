package com.zimbra.soap.json.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.annotate.JacksonAnnotation;

/**
 * This marker annotation can be used in Zimbra JAXB classes to affect how they are serialized to Zimbra style JSON.
 * Notes on {@code JSONElement}:
 * A JSON element added via {@code addElement} is always serialized as an array, because there could be
 * further {@code addElement} calls with the same element name.  On the other hand, a JSON element added via
 * {@code addUniqueElement} won't be serialized as an array as their is an implicit assumption that there will
 * be only one element with that name.
 * This marker goes with {@code @XmlElement/@XmlElementRef} annotations to flag that they should be
 * serialized in the same way as a JSONElement which has been added via {@code addUniqueElement}
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface ZimbraUniqueElement
{
    /**
     * Optional argument that defines whether this annotation is active or not. The only use for value 'false' is
     * for overriding purposes.
     * Overriding may be necessary when used with "mix-in annotations" (aka "annotation overrides").
     * For most cases, however, default value of "true" is just fine and should be omitted.
     */
    boolean value() default true;
}
