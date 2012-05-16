/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;

import org.codehaus.jackson.annotate.JacksonAnnotation;

/**
 * <p>Marker annotation used in Zimbra JAXB classes to affect how they are serialized to Zimbra style JSON.</p>
 * <h1>Notes on {@link Element}:</h1>
 * If addAttribute is called with {@link Element.Disposition.CONTENT} similarly to:
 * <pre>
 *     elem.addAttribute("soapURL", "http://fun.example.test", Element.Disposition.CONTENT);
 * </pre>
 * then if 'elem' is a {@link XMLElement}, this is serialized as:
 * <pre>
 *     &lt;soapURL>https://soap.example.test&lt;/soapURL>
 * </pre>
 * and if 'elem' is a {@link JSONElement}, this is serialized as:
 * <pre>
 *     "soapURL": "https://soap.example.test"
 * </pre>
 * In other words, for the XML case, this is treated as adding an element - i.e. the JAXB annotation
 * {@link XmlElement} (or {@link XmlElementRef}) is appropriate.  However, for Zimbra JSON, string fields in JAXB
 * classes with the {@link XmlElement} annotation are normally serialized as:
 * <pre>
 *    "soapURL": [{ "_content": "http://fun.example.test" }]
 * </pre>
 * Adding this {@link ZimbraJsonAttribute} annotation to the field will cause it to be serialized similarly to this
 * instead:
 * <pre>
 *    "soapURL": "http://fun.example.test"
 * <pre/>
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface ZimbraJsonAttribute
{
    /**
     * Optional argument that defines whether this annotation is active or not. The only use for value 'false' is
     * for overriding purposes.
     * Overriding may be necessary when used with "mix-in annotations" (aka "annotation overrides").
     * For most cases, however, default value of "true" is just fine and should be omitted.
     */
    boolean value() default true;
}
