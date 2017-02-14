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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.RightViaInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHECK_RIGHT_RESPONSE)
public class CheckRightResponse {

    /**
     * @zm-api-field-description Result of the CheckRightRequest
     */
    @XmlAttribute(name=AdminConstants.A_ALLOW /* allow */, required=true)
    private final ZmBoolean allow;

    /**
     * @zm-api-field-description Via information for the grant that decisively lead to the result
     */
    @XmlElement(name=AdminConstants.E_VIA /* via */, required=false)
    private final RightViaInfo via;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckRightResponse() {
        this(false, (RightViaInfo) null);
    }

    public CheckRightResponse(boolean allow, RightViaInfo via) {
        this.allow = ZmBoolean.fromBool(allow);
        this.via = via;
    }

    public boolean getAllow() { return ZmBoolean.toBool(allow); }
    public RightViaInfo getVia() { return via; }
}
