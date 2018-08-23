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
package com.zimbra.doc.soap.apidesc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zimbra.doc.soap.ValueDescription;
import com.zimbra.doc.soap.XmlAttributeDescription;
import com.zimbra.soap.JaxbUtil;



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
