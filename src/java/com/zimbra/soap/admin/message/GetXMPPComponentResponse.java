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
import com.zimbra.soap.admin.type.XMPPComponentInfo;

/**
 * @zm-api-response-description Note:
 * <br />
 * Attributes that are not allowed to be got by the authenticated admin will be returned as :
 * <pre>
 *     &lt;a n=&quot;{attr-name}&quot; pd=&quot;1&quot;/&gt;
 * </pre>
 * To allow an admin to get all attributes, grant the getXMPPComponent right
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_XMPPCOMPONENT_RESPONSE)
public class GetXMPPComponentResponse {

    /**
     * @zm-api-field-description XMPP Component Information
     */
    @XmlElement(name=AccountConstants.E_XMPP_COMPONENT, required=true)
    private final XMPPComponentInfo component;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetXMPPComponentResponse() {
        this((XMPPComponentInfo) null);
    }

    public GetXMPPComponentResponse(XMPPComponentInfo component) {
        this.component = component;
    }

    public XMPPComponentInfo getComponent() { return component; }
}
