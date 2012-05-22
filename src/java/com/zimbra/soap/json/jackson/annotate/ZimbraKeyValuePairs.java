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
import org.codehaus.jackson.annotate.JacksonAnnotation;

/**
 * <p>Marker annotation used in Zimbra JAXB classes to affect how they are serialized to Zimbra style JSON.</p>
 * The {@code ZimbraKeyValuePairs} in the following code snippet:
 * <pre>
 *     @ZimbraKeyValuePairs
 *     @XmlElement(name=Element.XMLElement.E_ATTRIBUTE, required=false)
 *     private final List<KeyValuePair> attrList;
 * </pre>
 * causes serialization to JSON in the form :
 * <pre>
 * "_attrs": {
 *         "mail": "fun@example.test",
 *         "zimbraMailStatus": "enabled"
 *       }
 * </pre>
 * instead of:
 * <pre>
 * "a": [
 *          {
 *            "n": "mail",
 *            "_content": "fun@example.test"
 *          },
 *          {
 *            "n": "zimbraMailStatus",
 *            "_content": "enabled"
 *          }
 *      ]
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface ZimbraKeyValuePairs
{
    /**
     * Optional argument that defines whether this annotation is active or not. The only use for value 'false' is
     * for overriding purposes.
     * Overriding may be necessary when used with "mix-in annotations" (aka "annotation overrides").
     * For most cases, however, default value of "true" is just fine and should be omitted.
     */
    boolean value() default true;
}
