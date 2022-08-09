/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.HsmConstants;
import com.zimbra.soap.admin.type.Name;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Schedule Storage Management Policy
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=HsmConstants.E_SCHEDULE_SMPOLICY_REQUEST)
public class ScheduleSMPolicyRequest {

    /**
     * @zm-api-field-description Server specification
     */
    @XmlElement(name=AdminConstants.E_SERVER /* server */, required=true)
    private final Name server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ScheduleSMPolicyRequest() {
        this((Name) null);
    }

    public ScheduleSMPolicyRequest(Name server) {
        this.server = server;
    }

    public Name getServer() {
        return server;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("server", server);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
