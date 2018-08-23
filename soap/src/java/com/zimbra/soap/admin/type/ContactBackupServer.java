/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactBackupServer {
    @XmlEnum
    public enum ContactBackupStatus {
        // case must match protocol
        started, error, stopped;

        public static ContactBackupStatus fromString(String s) throws ServiceException {
            try {
                return ContactBackupStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private String name;
    @XmlAttribute(name=AdminConstants.A_STATUS /* status */, required=true)
    private ContactBackupStatus contactBackupStatus;

    @SuppressWarnings("unused")
    private ContactBackupServer() {
     // empty private constructor
    }
    /**
     * @param name
     * @param contactBackupStatus
     */
    public ContactBackupServer(String name, ContactBackupStatus contactBackupStatus) {
        this.name = name;
        this.contactBackupStatus = contactBackupStatus;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the status
     */
    public ContactBackupStatus getStatus() {
        return contactBackupStatus;
    }
    /**
     * @param contactBackupStatus the status to set
     */
    public void setStatus(ContactBackupStatus contactBackupStatus) {
        this.contactBackupStatus = contactBackupStatus;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        if (this.name != null && !this.name.isEmpty()) {
            helper.add("name", this.name);
        }
        if (this.contactBackupStatus != null) {
            helper.add("status", this.contactBackupStatus);
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}