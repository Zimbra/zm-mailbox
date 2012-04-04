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
package com.zimbra.doc.soap.apidesc;

import com.zimbra.doc.soap.ValueDescription;
import com.zimbra.doc.soap.XmlAttributeDescription;
import com.zimbra.soap.JaxbUtil;

import org.codehaus.jackson.annotate.JsonIgnore;

public class SoapApiAttribute {
    private final String name;
    private final String description;
    private final boolean required;
    private ValueDescription valueType = null;
    private String jaxb = null;

    /* no-argument constructor needed for deserialization */
    @SuppressWarnings("unused")
    private SoapApiAttribute() {
        name = null;
        description = null;
        required = false;
    }

    public SoapApiAttribute(XmlAttributeDescription desc) {
        name = desc.getName();
        description = desc.getRawDescription();
        valueType = desc.getValueDescription();
        Class<?> klass;
        try {
            klass = Class.forName(valueType.getClassName());
            if (JaxbUtil.isJaxbType(klass)) {
                jaxb = valueType.getClassName();
                valueType = null;
            }
        } catch (ClassNotFoundException e) {
            klass = null;
        }
        required = desc.isRequired();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
    public ValueDescription getValueType() { return valueType; }
    public String getJaxb() { return jaxb; }

    @JsonIgnore
    public boolean isSame(SoapApiAttribute other) {
        if (other == null) {
            return false;
        }
        return true;
    }
}
