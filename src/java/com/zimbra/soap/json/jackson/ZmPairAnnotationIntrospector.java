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
package com.zimbra.soap.json.jackson;

import javax.xml.bind.annotation.XmlValue;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import com.zimbra.common.soap.Element;

    /**
     * Zimbra specific annotation introspector.  Mostly based on a pair of annotation introspectors
     * (JacksonAnnotationIntrospector/JaxbAnnotationIntrospector)
     * 
     * @XmlValue handling - Zimbra uses the property name "_content" (JaxbAnnotationIntrospector uses "value")
     * 
     * Enum value handling - Use JaxbAnnotationIntrospector's handling in preference to JacksonAnnotationIntrospector's
     */
public final class ZmPairAnnotationIntrospector
extends AnnotationIntrospector.Pair {
    public ZmPairAnnotationIntrospector() {
        super(new JacksonAnnotationIntrospector(), new JaxbAnnotationIntrospector());
    }

    /**
     * Pair's findEnumValue won't try for the secondary's results if the primary's results are non-null, but
     * we need the XmlEnumValue values if available - which only the JaxbAnnotationIntrospector can provide
     */
    @Override
    public String findEnumValue(Enum<?> e) {
        return super._secondary.findEnumValue(e);
    }

    /**
     * We use "_content" for @XmlValue, so need to over-ride JaxbAnnotationIntrospector default "value"
     * Note: We skip any visibility check if the @XmlValue annotation is present - but that should be safe.
     */
    @Override
    public String findGettablePropertyName(AnnotatedMethod am) {
        String name = findJaxbValueName(am);
        return (name != null) ? name : super.findGettablePropertyName(am);
    }

    /**
     * We use "_content" for @XmlValue, so need to over-ride JaxbAnnotationIntrospector default "value"
     * Note: We skip any visibility check if the @XmlValue annotation is present - but that should be safe.
     */
    @Override
    public String findSerializablePropertyName(AnnotatedField af) {
        String name = findJaxbValueName(af);
        return (name != null) ? name : super.findSerializablePropertyName(af);
    }

    /**
     * We use "_content" for @XmlValue, so need to over-ride JaxbAnnotationIntrospector default "value"
     * Note: We skip any visibility check if the @XmlValue annotation is present - but that should be safe.
     */
    @Override
    public String findSettablePropertyName(AnnotatedMethod am) {
        String name = findJaxbValueName(am);
        return (name != null) ? name : super.findSettablePropertyName(am);
    }

    /**
     * We use "_content" for @XmlValue, so need to over-ride JaxbAnnotationIntrospector default "value"
     * Note: We skip any visibility check if the @XmlValue annotation is present - but that should be safe.
     */
    @Override
    public String findDeserializablePropertyName(AnnotatedField af) {
        String name = findJaxbValueName(af);
        return (name != null) ? name : super.findDeserializablePropertyName(af);
    }

    /**
     * @return "_content" if the XmlValue annotation is found, otherwise null
     */
    private static String findJaxbValueName(Annotated ae) {
        XmlValue valueInfo = ae.getAnnotation(XmlValue.class);
        return (valueInfo != null) ? Element.JSONElement.A_CONTENT : null;
    }

}
