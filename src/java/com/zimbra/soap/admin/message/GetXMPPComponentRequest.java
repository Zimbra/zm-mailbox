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
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.admin.type.XMPPComponentSelector;

/**
 * @zm-api-command-description Get XMPP Component
 * <br />
 * XMPP stands for Extensible Messaging and Presence Protocol
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_XMPPCOMPONENT_REQUEST)
public class GetXMPPComponentRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-description XMPP Component selector
     */
    @XmlElement(name=AccountConstants.E_XMPP_COMPONENT, required=true)
    private final XMPPComponentSelector component;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetXMPPComponentRequest() {
        this((XMPPComponentSelector) null);
    }

    public GetXMPPComponentRequest(XMPPComponentSelector component) {
        this.component = component;
    }

    public XMPPComponentSelector getComponent() { return component; }
}
