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
package com.zimbra.soap.json.jackson;

import java.lang.reflect.Field;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.zimbra.common.soap.Element;

    /**
     * Zimbra specific annotation introspector.  Mostly based on a pair of annotation introspectors
     * (JacksonAnnotationIntrospector/ZimbraJaxbAnnotationIntrospector)
     * ZimbraJaxbAnnotationIntrospector is a hopefully temporary wrapper round JaxbAnnotationIntrospector
     * to get around a bug.
     *
     * @XmlValue handling - Zimbra uses the property name "_content" (JaxbAnnotationIntrospector uses "value")
     *
     * Enum value handling - Use JaxbAnnotationIntrospector's handling in preference to JacksonAnnotationIntrospector's
     */
public final class ZmPairAnnotationIntrospector extends AnnotationIntrospectorPair {
        private static final long serialVersionUID = -3110570221665228877L;

    public ZmPairAnnotationIntrospector() {
        super(new JacksonAnnotationIntrospector(),
            new ZimbraJaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
    }

    @Override // since 2.7
    public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        HashMap<String,String> expl = null;
        for (Field f : ClassUtil.getDeclaredFields(enumType)) {
            if (!f.isEnumConstant()) {
                continue;
            }
            XmlEnumValue enumValue = f.getAnnotation(XmlEnumValue.class);
            if (enumValue == null) {
                continue;
            }
            String n = enumValue.value();
            if (expl == null) {
                expl = new HashMap<String,String>();
            }
            expl.put(f.getName(), n);
        }
        // and then stitch them together if and as necessary
        if (expl != null) {
            int end = enumValues.length;
            for (int i = 0; i < end; ++i) {
                String defName = enumValues[i].name();
                String explValue = expl.get(defName);
                if (explValue != null) {
                    names[i] = explValue;
                }
            }
        }
        return names;
    }

    @Override
    public PropertyName findNameForSerialization(Annotated am) {
        String name = findJaxbValueName(am);
        PropertyName propName = new PropertyName(name);
        propName = (!propName.isEmpty()) ? propName : super.findNameForSerialization(am);
        return propName;
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated af) {
        String name = findJaxbValueName(af);
        PropertyName propName = new PropertyName(name);
        return (!propName.isEmpty()) ? propName : super.findNameForDeserialization(af);
    }

    /**
     * @return "_content" if the XmlValue annotation is found, otherwise null
     */
    private static String findJaxbValueName(Annotated ae) {
        XmlValue valueInfo = ae.getAnnotation(XmlValue.class);
        return (valueInfo != null) ? Element.JSONElement.A_CONTENT : null;
    }

    @Override
    public String findPropertyDefaultValue(Annotated ann) {
        String name = findJaxbValueName(ann);
      return (name != null) ? name : super.findPropertyDefaultValue(ann);
    }
}
