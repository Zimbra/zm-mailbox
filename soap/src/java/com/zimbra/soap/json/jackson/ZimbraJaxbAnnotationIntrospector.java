/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * This class exists because of:
 *     https://github.com/FasterXML/jackson-modules-base/issues/47
 * Perhaps when that is fixed, this can go away.
 */
public class ZimbraJaxbAnnotationIntrospector extends JaxbAnnotationIntrospector {
    private static final long serialVersionUID = 3903948048784286612L;

    public ZimbraJaxbAnnotationIntrospector(TypeFactory typeFactory) {
        super(typeFactory);
    }

    @Override
    public PropertyName findNameForSerialization(Annotated a)
    {
        PropertyName propertyName = super.findNameForSerialization(a);
        if (propertyName == null) {
            return propertyName;
        }
        PropertyName pn = null;
        if (a instanceof AnnotatedMethod) {
            AnnotatedMethod am = (AnnotatedMethod) a;
            pn = xmlElementWrapperName(am);
        }
        if (a instanceof AnnotatedField) {
            AnnotatedField af = (AnnotatedField) a;
            pn = xmlElementWrapperName(af);
        }
        return pn != null ? pn : propertyName;
    }

    private PropertyName xmlElementWrapperName(Annotated ae) {
        XmlElement element = ae.getAnnotation(XmlElement.class);
        if (element == null) {
            return null;
        }
        XmlElementWrapper wrapper = ae.getAnnotation(XmlElementWrapper.class);
        return (wrapper == null) ? null : combineNames(wrapper.name(), wrapper.namespace());
    }

    private static PropertyName combineNames(String localName, String namespace)
    {
        if (MARKER_FOR_DEFAULT.equals(localName)) {
            return null;
        }
        if (MARKER_FOR_DEFAULT.equals(namespace)) {
            return new PropertyName(localName);
        }
        return new PropertyName(localName, namespace);
    }
}
