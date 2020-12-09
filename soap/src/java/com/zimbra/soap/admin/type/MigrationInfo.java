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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class MigrationInfo {

    /**
     * @zm-api-field-tag sourceUser
     * @zm-api-field-description source System user account from where data will be migrated
     */
    @XmlAttribute(name = AdminConstants.A_SOURCE_USER, required = true)
    private String sourceUser;

    /**
     * @zm-api-field-tag targetUser
     * @zm-api-field-description Target system user account to where data will be migrated
     */
    @XmlAttribute(name = AdminConstants.A_TARGET_USER, required = true)
    private String targetUser;

    /**
     * @zm-api-field-tag typesOfdata
     * @zm-api-field-description Which types of data is to be migrated.
     * e.g; imap(mail), caldav(calendar), contact, file(document), task
     */
    @XmlAttribute(name = AdminConstants.A_TYPES_OF_DATA, required = true)
    private String typesOfData;

    public MigrationInfo() {
        this ((String) null, (String) null, (String) null);
    }

    public MigrationInfo(String sUser, String tUser, String types) {
        this.sourceUser = sUser;
        this.targetUser = tUser;
        this.typesOfData = types;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getTypesOfData() {
        return typesOfData;
    }

    public void setTypesOfData(String typesOfData) {
        this.typesOfData = typesOfData;
    }
}
