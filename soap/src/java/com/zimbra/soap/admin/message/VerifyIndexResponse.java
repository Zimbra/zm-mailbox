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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_VERIFY_INDEX_RESPONSE)
@XmlType(propOrder = {"status", "message"})
public class VerifyIndexResponse {

    /**
     * @zm-api-field-tag verify-result-status
     * @zm-api-field-description Result status of verification.  Valid values "true" and "false" (Not "1" and "0")
     */
    @XmlElement(name=AdminConstants.E_STATUS /* status */, required=true)
    private final ZmBoolean status;

    /**
     * @zm-api-field-tag verification-output
     * @zm-api-field-description Verification output
     */
    @XmlElement(name=AdminConstants.E_MESSAGE /* message */, required=true)
    private final String message;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private VerifyIndexResponse() {
        this(false,(String) null);
    }

    public VerifyIndexResponse(boolean status, String message) {
        this.status = ZmBoolean.fromBool(status);
        this.message = message;
    }

    public boolean isStatus() { return ZmBoolean.toBool(status); }
    public String getMessage() { return message; }
}
