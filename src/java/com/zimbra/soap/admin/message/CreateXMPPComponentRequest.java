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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.XMPPComponentSpec;

/**
 * @zm-api-command-description Create an XMPP component
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_XMPPCOMPONENT_REQUEST)
public class CreateXMPPComponentRequest {

    /**
     * @zm-api-field-description XMPP Component details
     */
    @XmlElement(name=AccountConstants.E_XMPP_COMPONENT, required=true)
    private final XMPPComponentSpec component;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateXMPPComponentRequest() {
        this((XMPPComponentSpec) null);
    }

    public CreateXMPPComponentRequest(XMPPComponentSpec component) {
        this.component = component;
    }

    public XMPPComponentSpec getComponent() { return component; }
}
