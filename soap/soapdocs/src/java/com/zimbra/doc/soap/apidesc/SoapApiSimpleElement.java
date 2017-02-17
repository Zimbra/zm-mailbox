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

import com.zimbra.doc.soap.XmlElementDescription;
import com.zimbra.soap.util.JaxbInfo;

public class SoapApiSimpleElement
implements SoapApiNamedElement {
    private final String name;
    private final String namespace;
    private final String type;
    private final String description;
    private final boolean required;
    private final boolean list;

    /* no-argument constructor needed for deserialization */
    protected SoapApiSimpleElement() {
        name = null;
        namespace = null;
        type = null;
        description = null;
        required = false;
        list = false;
    }

    public SoapApiSimpleElement(XmlElementDescription descNode) {
        name = descNode.getName();
        String ns = descNode.getTargetNamespace();
        if (ns == null || (JaxbInfo.DEFAULT_MARKER.equals(ns))) {
            namespace = null;
        } else {
            namespace = ns;
        }
        description = descNode.getRawDescription();
        list = !descNode.isSingleton();
        required = !descNode.isOptional();
        type = descNode.getTypeName();
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getNamespace() { return namespace; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
    public boolean isList() { return list; }
    public String getType() { return type; }
    public String getJaxb() { return null; }
}
