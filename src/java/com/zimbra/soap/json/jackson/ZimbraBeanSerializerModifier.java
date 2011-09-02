/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializerModifier;
import org.codehaus.jackson.type.JavaType;

/**
 * We need a {@link BeanSerializerModifier} to replace default <code>BeanSerializer</code>
 * with Zimbra-specific one; mostly to ensure that @XmlElementWrapper wrapped lists
 * are handled in the Zimbra way.
 */
public class ZimbraBeanSerializerModifier extends BeanSerializerModifier
{
    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    /**
     * First thing to do is to find annotations regarding XML serialization,
     * and wrap collection serializers.
     */
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
            BasicBeanDescription beanDesc, List<BeanPropertyWriter> beanProperties)
    {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0, len = beanProperties.size(); i < len; ++i) {
            BeanPropertyWriter bpw = beanProperties.get(i);
            final AnnotatedMember member = bpw.getMember();
            QName wrapperName = findWrapperName(intr, member);
            if (wrapperName == null) {
                continue;
            }
            String localName = wrapperName.getLocalPart();
            if (localName == null || localName.length() == 0) {
                continue;
            }
            if (!_isContainerType(bpw.getType())) {
                continue;
            }
            QName wrappedName = ZimbraBeanSerializerModifier.findXmlElementName(intr, member);
            if (wrappedName == null) {
                wrappedName = new QName("", bpw.getName());
            }
            beanProperties.set(i, new ZimbraBeanPropertyWriter(bpw, wrapperName, wrappedName));
        }
        return beanProperties;
    }

    /**
     * Helper method used for figuring out if given raw type is a collection ("indexed") type;
     * in which case a wrapper element is typically added.
     */
    private static boolean _isContainerType(JavaType type)
    {
        if (type.isContainerType()) {
            Class<?> cls = type.getRawClass();
            // One special case; byte[] will be serialized as base64-encoded String, not real array, so:
            // (actually, ditto for char[]; thought to be a String)
            if (cls == byte[].class || cls == byte[].class) {
                return false;
            }
            // issue#5: also, should not add wrapping for Maps
            if (Map.class.isAssignableFrom(cls)) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static QName findWrapperName(AnnotationIntrospector ai, AnnotatedMember prop)
    {
        JsonSerialize jsonSer = prop.getAnnotation(JsonSerialize.class);
        if (jsonSer != null) {
            /* if we have over-ridden the serialization - assume it has handled any required wrapping */
            return null;
        }
        XmlElementWrapper wrapper = prop.getAnnotation(XmlElementWrapper.class);
        if (wrapper == null) {
            return null;
        }
        return new QName(wrapper.namespace(),wrapper.name());
    }

    private static QName findXmlElementName(AnnotationIntrospector ai, AnnotatedMember prop)
    {
        JsonSerialize jsonSer = prop.getAnnotation(JsonSerialize.class);
        if (jsonSer != null) {
            /* if we have over-ridden the serialization - assume it has handled any required wrapping */
            return null;
        }
        XmlElement elemAnnot = prop.getAnnotation(XmlElement.class);
        if (elemAnnot == null) {
            return null;
        }
        return new QName(elemAnnot.namespace(), elemAnnot.name());
    }
}
