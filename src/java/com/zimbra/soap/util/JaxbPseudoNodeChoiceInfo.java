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

package com.zimbra.soap.util;

import java.lang.reflect.Type;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.google.common.collect.Lists;

/**
 * Place holder to store information related to XmlElements or XmlElementRefs annotations
 * i.e. Captures the fact that there is a choice between various elements at this point
 */
public final class JaxbPseudoNodeChoiceInfo
implements JaxbNodeInfo {
    private boolean canHaveMultipleElements;
    private String fieldName;
    private final List <JaxbElementInfo> elems = Lists.newArrayList();

    public JaxbPseudoNodeChoiceInfo(String fieldName, Type defaultGenericType) {
        this.fieldName = fieldName;
        canHaveMultipleElements = JaxbInfo.representsMultipleElements(defaultGenericType);
    }

    public JaxbElementInfo getElemInfo(String name) {
        for (JaxbElementInfo entry : elems) {
            if (name.equals(entry.getName())) {
                return entry;
            }
        }
        return null;
    }

    public boolean hasElement(String name) {
        return (getElemInfo(name) != null);
    }

    public Iterable<String> getElementNames() {
        List<String> elemNames = Lists.newArrayList();
        for (JaxbNodeInfo entry : elems) {
            elemNames.add(entry.getName());
        }
        return elemNames;
    }

    public Class<?> getClassForElementName(String name) {
        JaxbElementInfo info = getElemInfo(name);
        return info == null ? null : info.getAtomClass();
    }

    public Iterable<JaxbElementInfo> getElements() {
        return elems;
    }

    public void add(XmlElement elem) {
        JaxbElementInfo info = new JaxbElementInfo(elem, fieldName, null);
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null) {
            elems.add(info);
        }
    }

    public void add(XmlElementRef elemRef) {
        JaxbElementInfo info = new JaxbElementInfo(elemRef, fieldName, null);
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null) {
            elems.add(info);
        }
    }

    @Override
    public String getName() { return null; }
    @Override
    public String getNamespace() { return null; }
    @Override
    public boolean isRequired() { return true; }
    @Override
    public boolean isMultiElement() { return canHaveMultipleElements; }

    public String getFieldName() { return fieldName; }
}
