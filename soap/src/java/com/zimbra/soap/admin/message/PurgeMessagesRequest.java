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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Purges aged messages out of trash, spam, and entire mailbox
 * <br />
 * (if <b>&lt;mbox></b> element is omitted, purges all mailboxes on server)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_PURGE_MESSAGES_REQUEST)
public class PurgeMessagesRequest {

    /**
     * @zm-api-field-description Mailbox selector
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=false)
    private final MailboxByAccountIdSelector mbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PurgeMessagesRequest() {
        this((MailboxByAccountIdSelector)null);
    }

    public PurgeMessagesRequest(String accountId) {
        this(new MailboxByAccountIdSelector(accountId));
    }

    public PurgeMessagesRequest(MailboxByAccountIdSelector mbox) {
        this.mbox = mbox;
    }

    public MailboxByAccountIdSelector getMbox() { return mbox; }
}
