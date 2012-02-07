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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.PurgeMovedMailboxInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_PURGE_MOVED_MAILBOX_RESPONSE)
@XmlType(propOrder = {})
public class PurgeMovedMailboxResponse {

    /**
     * @zm-api-field-description Information about purged mailbxo
     */
    @XmlElement(name=BackupConstants.E_MAILBOX /* mbox */, required=true)
    private final PurgeMovedMailboxInfo mailbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PurgeMovedMailboxResponse() {
        this((PurgeMovedMailboxInfo) null);
    }

    public PurgeMovedMailboxResponse(PurgeMovedMailboxInfo mailbox) {
        this.mailbox = mailbox;
    }

    public PurgeMovedMailboxInfo getMailbox() { return mailbox; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("mailbox", mailbox);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
