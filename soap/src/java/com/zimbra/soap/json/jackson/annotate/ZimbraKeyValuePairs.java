/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.json.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;


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
