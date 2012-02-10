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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class PurgeMovedMailboxInfo {

    /**
     * @zm-api-field-tag server-hostname
     * @zm-api-field-description Hostname of server the purge took place on
     */
    @XmlAttribute(name=BackupConstants.A_SERVER /* server */, required=true)
    private final String server;

    /**
     * @zm-api-field-tag purged-mailbox-id
     * @zm-api-field-description Purged mailbox ID
     */
    @XmlAttribute(name=BackupConstants.A_MAILBOXID /* mbxid */, required=true)
    private final int mailboxId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PurgeMovedMailboxInfo() {
        this((String) null, -1);
    }

    public PurgeMovedMailboxInfo(String server, int mailboxId) {
        this.server = server;
        this.mailboxId = mailboxId;
    }

    public String getServer() { return server; }
    public int getMailboxId() { return mailboxId; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("mailboxId", mailboxId);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
