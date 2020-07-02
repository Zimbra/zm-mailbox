/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.google.common.collect.Maps;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.json.jackson.annotate.ZimbraKeyValuePairs;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.util.JaxbInfo;

/**
 * Used to store field name information to be used in Zimbra-style JSON related to a JAXB object field.
 * A single field in a JAXB object may have a single field name e.g. from @XmlElement or several possible names
 * e.g. from @XmlElements
 * In addition, an @XmlElementWrapper annotation requires the resulting Zimbra-style JSON to add a wrapping
 * field
 */
public class NameInfo {
    
    private QName wrapperName = null;
    private QName wrappedName = null;
    private Map <Class<?>,QName> wrappedNameMap = null;
    private boolean treatAsUniqueElement = false;
    private boolean wrapperIsArray = false;
    private boolean treatAsAttribute = false;
    private boolean mixedAllowed = false;
    private boolean anyElementAllowed = false;
    private boolean anyAttributeAllowed = false;
    private boolean keyValuePairs = false;

    public NameInfo(AnnotationIntrospector ai, AnnotatedMember prop, String defaultWrappedName) {
        JsonSerialize jsonSer = prop.getAnnotation(JsonSerialize.class);
        if (jsonSer != null) {
            /* if we have over-ridden the serialization - assume it has handled any required wrapping */
            return;
        }
        setWrapperInfo(ai, prop);
        setWrappedInfo(ai, prop, defaultWrappedName);
    }

    private void setWrapperInfo(AnnotationIntrospector ai, AnnotatedMember prop)
    {
        XmlElementWrapper wrapper = prop.getAnnotation(XmlElementWrapper.class);
        if (wrapper == null) {
            return;
        }
        wrapperName = new QName(wrapper.namespace(),wrapper.name());
    }

    private void setWrappedInfo(AnnotationIntrospector ai, AnnotatedMember prop, String defaultWrappedName) {
        ZimbraUniqueElement uniqueElemAnnot = prop.getAnnotation(ZimbraUniqueElement.class);
        if (uniqueElemAnnot != null) {
            treatAsUniqueElement = uniqueElemAnnot.value();
        }
        ZimbraJsonArrayForWrapper arrayForWrapperAnnot = prop.getAnnotation(ZimbraJsonArrayForWrapper.class);
        if (arrayForWrapperAnnot != null) {
            wrapperIsArray = arrayForWrapperAnnot.value();
        }
        ZimbraKeyValuePairs kvpAnnot = prop.getAnnotation(ZimbraKeyValuePairs.class);
        if (kvpAnnot != null) {
            keyValuePairs = kvpAnnot.value();
            return;
        }
        ZimbraJsonAttribute jsonAttributeAnnot = prop.getAnnotation(ZimbraJsonAttribute.class);
        if (jsonAttributeAnnot != null) {
            treatAsAttribute = jsonAttributeAnnot.value();
        }
        mixedAllowed = (prop.getAnnotation(XmlMixed.class) != null);
        anyElementAllowed = (prop.getAnnotation(XmlAnyElement.class) != null);
        anyAttributeAllowed = (prop.getAnnotation(XmlAnyAttribute.class) != null);

        XmlElement elemAnnot = prop.getAnnotation(XmlElement.class);
        if (elemAnnot != null) {
            wrappedName = getQName(prop.getName(), elemAnnot.namespace(), elemAnnot.name());
            return;
        }
        XmlElementRef elemRefAnnot = prop.getAnnotation(XmlElementRef.class);
        wrappedName = getElementRefName(elemRefAnnot);
        if (wrappedName != null) {
            return;
        }
        XmlElements elemsAnnot = prop.getAnnotation(XmlElements.class);
        if (elemsAnnot != null) {
            XmlElement[] elems = elemsAnnot.value();
            wrappedNameMap = Maps.newHashMapWithExpectedSize(elems.length);
            for (XmlElement elem : elems) {
                QName qn = getQName(prop.getName(), elem.namespace(), elem.name());
                Class<?> kls = elem.type();
                getWrappedNameMap().put(kls, qn);
            }
            return;
        }
        XmlElementRefs elemRefsAnnot = prop.getAnnotation(XmlElementRefs.class);
        if (elemRefsAnnot != null) {
            XmlElementRef[] elems = elemRefsAnnot.value();
            wrappedNameMap = Maps.newHashMapWithExpectedSize(elems.length);
            for (XmlElementRef elem : elems) {
                QName qn = getElementRefName(elem);
                Class<?> kls = elem.type();
                getWrappedNameMap().put(kls, qn);
            }
            return;
        }
        if (wrapperName != null) {
            // We have a wrapper but nothing to tell us what the wrapped name should be, so use default
            wrappedName = new QName("", defaultWrappedName);
        }
    }

    public QName getWrapperName() { return wrapperName; }
    public QName getWrappedName() { return wrappedName; }

    public boolean isTreatAsUniqueElement() { return treatAsUniqueElement; }
    public boolean isTreatAsAttribute() { return treatAsAttribute; }
    public boolean isMixedAllowed() { return mixedAllowed; }
    public boolean isAnyElementAllowed() { return anyElementAllowed; }
    public boolean isAnyAttributeAllowed() { return anyAttributeAllowed; }
    public boolean isKeyValuePairs() { return keyValuePairs; }
    public boolean isWrapperIsArray() { return wrapperIsArray; }

    public Map <Class<?>,QName> getWrappedNameMap() {
        return wrappedNameMap;
    }

    private boolean haveSpecialWrappingInfo() {
        return (wrapperName != null) || (wrappedName != null) || (getWrappedNameMap() != null);
    }

    public boolean needSpecialHandling() {
        return haveSpecialWrappingInfo() || isMixedAllowed() || isAnyElementAllowed() || isAnyAttributeAllowed() ||
                isKeyValuePairs() || isWrapperIsArray();
    }

    private QName getQName(String fieldName, String namespace, String name) {
        if ((name == null) || (JaxbInfo.DEFAULT_MARKER.equals(name))) {
            return new QName(JaxbInfo.DEFAULT_MARKER, fieldName);
        } else {
            return new QName(namespace, name);
        }
    }

    private QName getElementRefName(XmlElementRef elemRefAnnot) {
        if (elemRefAnnot == null) {
            return null;
        }
        QName rName = null;
        Class<?> klass = elemRefAnnot.type();
        if (klass == null) {
            return null;
        }
        String name = null;
        String ns = null;
        /* if {@link XmlElementRef} type=JAXBElement.class then name and ns may be used but if the type is a JAXB
         * class, then the XmlRootElement on that class is used and any name/ns is ignored (counter intuitive IMHO!)
         */
        if (JAXBElement.class.isAssignableFrom(klass)) {
            name = elemRefAnnot.name();
            ns = elemRefAnnot.namespace();
            rName = new QName(ns, name);
        } else {
            rName = getRootElementName(klass);
        }
        return rName;
    }

    private QName getRootElementName(Class<?> klass) {
        if (klass == null) {
            return null;
        }
        QName rName = null;
        XmlRootElement reAnnot = klass.getAnnotation(XmlRootElement.class);
        if (reAnnot != null) {
            rName = new QName(reAnnot.namespace(), reAnnot.name());
        }
        return rName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("wrapperName=").append((wrapperName == null) ? "null" : wrapperName);
        if (wrappedName != null) {
            sb.append(";wrappedName=").append(wrappedName);
        } else {
            if (getWrappedNameMap() != null) {
                sb.append(";wrappedNameMap=");
                for ( Entry<Class<?>, QName> entry :getWrappedNameMap().entrySet()) {
                    sb.append("[").append(entry.getKey().getName()).append(",");
                    sb.append(entry.getValue().toString()).append("]\n");
                }
            }
        }
        return sb.toString();
    }
}
