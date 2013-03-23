/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.json.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.zimbra.common.soap.Element.JSONElement;

import org.codehaus.jackson.annotate.JacksonAnnotation;

/**
 * <p>Marker annotation used in Zimbra JAXB classes to affect how they are serialized to Zimbra style JSON.</p>
 * <h1>Notes on {@link JSONElement}:</h1>
 * A JSON element added via {@code addElement} is always serialized as an array, because there could be
 * further {@code addElement} calls with the same element name.  On the other hand, a JSON element added via
 * <br />{@code addUniqueElement}<br />
 * won't be serialized as an array as there is an implicit assumption that there will be only one element with that name.
 * This marker goes with {@link XmlElement} or {@link XmlElementRef} annotations to flag that they should be
 * serialized in the same way as a {@code JSONElement} which has been added via {@code addUniqueElement}
 *
 * A String field with {@link XmlElement} or {@link XmlElementRef} would normally be serialized similarly to:
 * <pre>
 *    "str-elem": [{ "_content": "element ONE" }]
 * </pre>
 * If the field also has this {@link ZimbraUniqueElement} annotation, it is serialized similarly to this instead:
 * <pre>
 *    "str-elem": { "_content": "element ONE" }
 * </pre>
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
