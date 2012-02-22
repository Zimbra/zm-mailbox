/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("lockOwnerId", lockOwnerId)
            .add("lockOwnerEmail", lockOwnerEmail)
            .add("lockOwnerTimestamp", lockOwnerTimestamp);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
