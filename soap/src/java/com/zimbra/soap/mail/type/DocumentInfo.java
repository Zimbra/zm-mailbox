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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DocumentInfo extends CommonDocumentInfo {

    /**
     * @zm-api-field-tag lock-owner-account-id
     * @zm-api-field-description Lock owner account ID
     */
    @XmlAttribute(name=MailConstants.A_LOCKOWNER_ID /* loid */, required=false)
    private String lockOwnerId;

    /**
     * @zm-api-field-tag lock-owner-account-email
     * @zm-api-field-description Lock owner account email address
     */
    @XmlAttribute(name=MailConstants.A_LOCKOWNER_EMAIL /* loe */, required=false)
    private String lockOwnerEmail;

    /**
     * @zm-api-field-tag lock-timestamp
     * @zm-api-field-description Lock timestamp
     */
    @XmlAttribute(name=MailConstants.A_LOCKTIMESTAMP /* lt */, required=false)
    private String lockOwnerTimestamp;

    public DocumentInfo() {
        this((String) null);
    }

    public DocumentInfo(String id) {
        super(id);
    }

    public void setLockOwnerId(String lockOwnerId) {
        this.lockOwnerId = lockOwnerId;
    }
    public void setLockOwnerEmail(String lockOwnerEmail) {
        this.lockOwnerEmail = lockOwnerEmail;
    }
    public void setLockOwnerTimestamp(String lockOwnerTimestamp) {
        this.lockOwnerTimestamp = lockOwnerTimestamp;
    }
    public String getLockOwnerId() { return lockOwnerId; }
    public String getLockOwnerEmail() { return lockOwnerEmail; }
    public String getLockOwnerTimestamp() { return lockOwnerTimestamp; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("lockOwnerId", lockOwnerId)
            .add("lockOwnerEmail", lockOwnerEmail)
            .add("lockOwnerTimestamp", lockOwnerTimestamp);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
