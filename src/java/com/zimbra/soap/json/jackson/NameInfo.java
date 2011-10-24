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

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.namespace.QName;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import com.google.common.collect.Maps;

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
        XmlElement elemAnnot = prop.getAnnotation(XmlElement.class);
        if (elemAnnot != null) {
            wrappedName = new QName(elemAnnot.namespace(), elemAnnot.name());
            return;
        }
        XmlElementRef elemRefAnnot = prop.getAnnotation(XmlElementRef.class);
        if (elemRefAnnot != null) {
            wrappedName = new QName(elemRefAnnot.namespace(), elemRefAnnot.name());
            return;
        }
        XmlElements elemsAnnot = prop.getAnnotation(XmlElements.class);
        if (elemsAnnot != null) {
            XmlElement[] elems = elemsAnnot.value();
            wrappedNameMap = Maps.newHashMapWithExpectedSize(elems.length);
            for (XmlElement elem : elems) {
                QName qn = new QName(elem.namespace(), elem.name());
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
                QName qn = new QName(elem.namespace(), elem.name());
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

    public QName getWrapperName() {
        return wrapperName;
    }

    public QName getWrappedName() {
        return wrappedName;
    }

    public Map <Class<?>,QName> getWrappedNameMap() {
        return wrappedNameMap;
    }

    public boolean haveSpecialWrappingInfo() {
        return (wrapperName != null) || (wrappedName != null) || (getWrappedNameMap() != null);
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
