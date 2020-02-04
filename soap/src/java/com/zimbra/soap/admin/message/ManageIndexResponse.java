/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MANAGE_INDEX_RESPONSE)
public class ManageIndexResponse {

    /**
     * @zm-api-field-tag status
     * @zm-api-field-description <b>started</b> when action is accepted
     */
    @XmlAttribute(name=AdminConstants.A_STATUS, required=true)
    private final String status;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ManageIndexResponse() {
        this((String)null);
    }

    public ManageIndexResponse(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
}
