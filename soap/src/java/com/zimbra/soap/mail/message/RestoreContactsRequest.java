/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_RESTORE_CONTACTS_REQUEST)
public class RestoreContactsRequest {

    /**
     * @zm-api-field-tag contact-backup
     * @zm-api-field-description Filename of contact backup file
     */
    @XmlAttribute(name = MailConstants.A_CONTACTS_BACKUP_FILE_NAME, required = true)
    private String contactsBackupFileName;

    /**
     * @zm-api-field-tag resolve
     * @zm-api-field-description Restore resolve action - one of <b>ignore|modify|replace|reset</b> <br/>
     * Default value - reset <br/>
     * <ul>
     * <li> ignore - In case of conflict, ignore the existing contact. Create new contact from backup file.
     * <li> modify - In case of conflict, merge the existing contact with contact in backup file.
     * <li> replace - In case of conflict, replace the existing contact with contact in backup file.
     * <li> reset - Delete all existing contacts and restore contacts from backup file.
     * </ul>
     */
    @XmlAttribute(name=MailConstants.A_CONTACTS_RESTORE_RESOLVE /* resolve */, required=false)
    private Resolve resolve;

    public Resolve getResolve() {
        return resolve;
    }

    public void setResolve(Resolve resolve) {
        this.resolve = resolve;
    }

    @XmlEnum
    public static enum Resolve {
        ignore, modify, replace, reset;

        public static Resolve fromString(String value) throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Resolve.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        "Invalid value: " + value + ", valid values: " + Arrays.asList(Resolve.values()), null);
            }
        }
    }

    public String getContactsBackupFileName() {
        return contactsBackupFileName;
    }

    public void setContactsBackupFileName(String contactsBackupFileName) {
        this.contactsBackupFileName = contactsBackupFileName;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("contactsBackupFileName", contactsBackupFileName).add("resolve", resolve);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
