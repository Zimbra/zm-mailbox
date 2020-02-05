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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get appointment ids since given id
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_APPOINTMENT_IDS_SINCE_REQUEST)
public class GetAppointmentIdsSinceRequest {
    /**
     * @zm-api-field-tag lastSync
     * @zm-api-field-description last synced appointment id
     */
    @XmlAttribute(name=MailConstants.A_CAL_LAST_SYNC /* lastSync */, required=true)
    private int lastSync;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID.
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=true)
    private String folderId;

    public GetAppointmentIdsSinceRequest() {
    }

    public GetAppointmentIdsSinceRequest(String folderId, int lastSync) {
        this.folderId = folderId;
        this.lastSync = lastSync;
    }

    /**
     * @return the lastSync
     */
    public int getLastSync() {
        return lastSync;
    }

    /**
     * @param lastSync the lastSync to set
     */
    public void setLastSync(int lastSync) {
        this.lastSync = lastSync;
    }

    /**
     * @return the folderId
     */
    public String getFolderId() {
        return folderId;
    }

    /**
     * @param folderId the folderId to set
     */
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
