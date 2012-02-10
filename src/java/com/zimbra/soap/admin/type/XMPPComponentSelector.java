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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class XMPPComponentSelector {

    /**
     * @zm-api-field-tag xmpp-comp-selector-by
     * @zm-api-field-description Select the meaning of <b>{xmpp-comp-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=true)
    private final XMPPComponentBy by;

    /**
     * @zm-api-field-tag xmpp-comp-selector-key
     * @zm-api-field-description The key used to identify the XMPP component.
     * Meaning determined by <b>{xmpp-comp-selector-by}</b>
     */
    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XMPPComponentSelector() {
        this((XMPPComponentBy) null, (String) null);
    }

    public XMPPComponentSelector(XMPPComponentBy by, String value) {
        this.by = by;
        this.value = value;
    }

    public XMPPComponentBy getBy() { return by; }
    public String getValue() { return value; }
}
