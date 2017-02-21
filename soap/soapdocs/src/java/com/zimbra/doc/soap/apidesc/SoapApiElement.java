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

public class SoapApiElement
extends SoapApiSimpleElement {
    private final String jaxb; /* Name of JAXB class associated with this element */

    /* no-argument constructor needed for deserialization */
    @SuppressWarnings("unused")
    private SoapApiElement() {
        super();
        jaxb = null;
    }

    public SoapApiElement(XmlElementDescription descNode) {
        super(descNode);
        Class<?> jaxbClass = descNode.getJaxbClass();
        jaxb = jaxbClass == null ? null : jaxbClass.getName();
    }

    @Override
    public String getJaxb() { return jaxb; }
    @Override
    public String getType() { return null; }
}
