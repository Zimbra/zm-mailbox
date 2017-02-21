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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class MailboxWithMailboxId {

    /**
     * @zm-api-field-tag mailbox-id
     * @zm-api-field-description Mailbox ID
     */
    @XmlAttribute(name=AdminConstants.A_MAILBOXID /* mbxid */, required=true)
    private final int mbxid;

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ACCOUNTID /* id */, required=false)
    private String accountId;

    // DeleteMailbox doesn't set this
    /**
     * @zm-api-field-tag size-in-bytes
     * @zm-api-field-description Size in bytes
     */
    @XmlAttribute(name=AdminConstants.A_SIZE /* s */, required=false)
    private final Long size;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MailboxWithMailboxId() {
        this(0, null, null);
    }

    public MailboxWithMailboxId(int mbxid, String accountId, Long size) {
        this.mbxid = mbxid;
        this.size = size;
        this.accountId = accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public int getMbxid() { return mbxid; }
    public Long getSize() { return size; }
    public String getAccountId() { return accountId; }
}
