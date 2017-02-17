/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
