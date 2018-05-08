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

package com.zimbra.common.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactBackupServer {
    @XmlEnum
    public enum Status {
        // case must match protocol
        started, error, stopped;

        public static Status fromString(String s) throws ServiceException {
            try {
                return Status.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private String name;
    @XmlAttribute(name=AdminConstants.A_STATUS /* status */, required=true)
    private Status status;

    @SuppressWarnings("unused")
    private ContactBackupServer() {
     // empty private constructor
    }
    /**
     * @param name
     * @param status
     */
    public ContactBackupServer(String name, Status status) {
        this.name = name;
        this.status = status;
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
    public Status getStatus() {
        return status;
    }
    /**
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        if (this.name != null && !this.name.isEmpty()) {
            helper.add("name", this.name);
        }
        if (this.status != null) {
            helper.add("status", this.status);
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}