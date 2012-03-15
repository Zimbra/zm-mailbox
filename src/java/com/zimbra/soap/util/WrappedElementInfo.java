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
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Represents an element that wraps other elements (i.e. one defined via {@code @XmlElementWrapper})
 */
public class WrappedElementInfo
implements JaxbNodeInfo {
    private String name;
    private String namespace;
    private String fieldName;
    private boolean required;
    private final List<JaxbNodeInfo> wrappedElems = Lists.newArrayList();

    public WrappedElementInfo(XmlElementWrapper annotation, String fieldName) {
        name = annotation.name();
        namespace = annotation.namespace();
        required = annotation.required();
        this.fieldName = fieldName;
    }

    public Iterable<String> getElementNames() {
        List<String> elemNames = Lists.newArrayList();
        for (JaxbNodeInfo node : wrappedElems) {
            if (node instanceof JaxbPseudoNodeChoiceInfo) {
                JaxbPseudoNodeChoiceInfo pseudoNode = (JaxbPseudoNodeChoiceInfo) node;
                Iterables.addAll(elemNames, pseudoNode.getElementNames());
            } else {
                elemNames.add(node.getName());
            }
        }
        return elemNames;
    }

    public Class<?> getClassForElementName(String name) {
        JaxbElementInfo node = getWrappedElem(name);
        if (node != null) {
            return node.getAtomClass();
        }
        return null;
    }

    public JaxbElementInfo getWrappedElem(String name) {
        for (JaxbNodeInfo node : wrappedElems) {
            if (node instanceof JaxbPseudoNodeChoiceInfo) {
                JaxbPseudoNodeChoiceInfo pseudoNode = (JaxbPseudoNodeChoiceInfo) node;
                JaxbElementInfo alternativeNode = pseudoNode.getElemInfo(name);
                if (alternativeNode != null) {
                    return alternativeNode;
                }
            } else if (node instanceof JaxbElementInfo) {
                if (name.equals(node.getName())) {
                    return (JaxbElementInfo) node;
                }
            }
        }
        return null;
    }

    public Iterable<JaxbNodeInfo> getElements() {
        return wrappedElems;
    }

    public void add(JaxbPseudoNodeChoiceInfo choiceNode) {
        wrappedElems.add(choiceNode);
    }
    
    public void add(XmlElement elem, String defaultName, Type defaultGenericType) {
        JaxbElementInfo info = new JaxbElementInfo(elem, fieldName, defaultGenericType);
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null) {
            wrappedElems.add(info);
        }
    }

    public void add(XmlElementRef elemRef, String defaultName, Type defaultGenericType) {
        JaxbElementInfo info = new JaxbElementInfo(elemRef, fieldName, defaultGenericType);
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null) {
            wrappedElems.add(info);
        }
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getNamespace() { return namespace; }
    @Override
    public boolean isRequired() { return required; }
    @Override
    public boolean isMultiElement() { return false; }
}
