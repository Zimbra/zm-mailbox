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
