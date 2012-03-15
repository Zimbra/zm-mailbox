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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

public class JaxbElementInfo
implements JaxbNodeInfo {
    private static final Log LOG = ZimbraLog.soap;
    private String name;
    private String namespace;
    private boolean required;
    private boolean canHaveMultipleElements;
    private String fieldName;
    private String stamp;
    private Class<?> atomClass;

    public JaxbElementInfo(XmlElement annot, String fieldName, Type defaultGenericType) {
        this.name = annot.name();
        this.namespace = annot.namespace();
        this.required = annot.required();
        this.fieldName = fieldName;
        if ((name == null) || JaxbInfo.DEFAULT_MARKER.equals(name)) {
            name = fieldName;
        }
        if (name == null) {
            throw new RuntimeException(
                String.format("Ignoring element with annotation %s unable to determine name", annot.toString()));
        }
        stamp = String.format("[element=%s]", name);
        atomClass = annot.type();
        if (atomClass == XmlElement.DEFAULT.class)
            atomClass = JaxbInfo.classFromType(defaultGenericType);
        if (atomClass == null) {
            LOG.debug("%s Unable to determine class for element with annotation '%s'", stamp, annot);
        }
        canHaveMultipleElements = JaxbInfo.representsMultipleElements(defaultGenericType);
    }

    public JaxbElementInfo(XmlElementRef annot, String fieldName, Type defaultGenericType) {
        name = annot.name();
        namespace = annot.namespace();
        this.fieldName = fieldName;
        required = true; // TODO annotation.required() does not exist!
        if ((name == null) || JaxbInfo.DEFAULT_MARKER.equals(name)) {
            name = JaxbInfo.getRootElementName(atomClass);
            if (name == null)
                name = fieldName;
        }
        if (name == null) {
            throw new RuntimeException(
                String.format("Ignoring element with annotation %s unable to determine name", annot.toString()));
        }
        stamp = String.format("[element=%s]", name);
        atomClass = annot.type();
        if (atomClass == XmlElementRef.DEFAULT.class)
            atomClass = JaxbInfo.classFromType(defaultGenericType);
        if (atomClass == null) {
            LOG.debug("%s Unable to determine name for element with annotation '%s'", stamp, annot);
        }
        canHaveMultipleElements = JaxbInfo.representsMultipleElements(defaultGenericType);
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getNamespace() { return namespace; }
    @Override
    public boolean isRequired() { return required; }
    @Override
    public boolean isMultiElement() { return canHaveMultipleElements; }

    public String getFieldName() { return fieldName; }
 
    /**
     * @return the class that would represent one XML Element (i.e. if associated with a List, want
     * the class associated with an element of the array - not List)
     */
    public Class<?> getAtomClass() { return atomClass; }
}
