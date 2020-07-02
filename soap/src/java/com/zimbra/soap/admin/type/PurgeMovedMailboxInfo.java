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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("mailboxId", mailboxId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
