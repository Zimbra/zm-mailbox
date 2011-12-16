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

package com.zimbra.soap.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Represents an element that wraps other elements (i.e. one defined via {@code @XmlElementWrapper})
 */
public class WrappedElementInfo
implements JaxbNodeInfo {
    private String name;
    private String namespace;
    private String fieldName;
    private boolean required;
    private HashMap<String,JaxbElementInfo> wrappedElems;

    public WrappedElementInfo(XmlElementWrapper annotation, String fieldName) {
        name = annotation.name();
        namespace = annotation.namespace();
        required = annotation.required();
        this.fieldName = fieldName;
        wrappedElems = Maps.newHashMap();
    }

    public Iterable<String> getElementNames() {
        return this.wrappedElems.keySet();
    }

    public Class<?> getClassForElementName(String name) {
        JaxbElementInfo info = this.wrappedElems.get(name);
        return info == null ? null : info.getAtomClass();
    }

    public Iterable<JaxbElementInfo> getElements() {
        List<JaxbElementInfo> elems = Lists.newArrayList();
        Iterables.addAll(elems, this.wrappedElems.values());
        return elems;
    }

    public void add(XmlElement elem, String defaultName, Type defaultGenericType) {
        JaxbElementInfo info = new JaxbElementInfo(elem, fieldName, defaultGenericType);
        String childName = info.getName();
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null && !Strings.isNullOrEmpty(name)) {
            wrappedElems.put(childName, info);
        }
    }

    public void add(XmlElementRef elemRef, String defaultName, Type defaultGenericType) {
        JaxbElementInfo info = new JaxbElementInfo(elemRef, fieldName, defaultGenericType);
        String childName = info.getName();
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null && !Strings.isNullOrEmpty(name)) {
            wrappedElems.put(childName, info);
        }
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getNamespace() { return namespace; }
    @Override
    public boolean isRequired() { return required; }
    @Override
    public boolean isArray() { return false; }
}
