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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

/**
 * @zm-api-command-description Delete a mailbox
 * <br />
 * The request includes the account ID (uuid) of the target mailbox on success, the response includes the mailbox
 * ID (numeric) of the deleted mailbox the <b>&lt;mbox></b> element is left out of the response if no mailbox existed
 * for that account.
 * <br />
 * Note: this request is by default proxied to the account's home server
 * <br />
 * <br />
 * <b>Access</b>: domain admin sufficient

 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DELETE_MAILBOX_REQUEST)
public class DeleteMailboxRequest {

    /**
     * @zm-api-field-description Mailbox
     */
    @XmlElement(name=AdminConstants.E_MAILBOX /* mbox */, required=false)
    private final MailboxByAccountIdSelector mbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DeleteMailboxRequest() {
        this((MailboxByAccountIdSelector) null);
    }

    public DeleteMailboxRequest(String accountId) {
        this(new MailboxByAccountIdSelector(accountId));
    }

    public DeleteMailboxRequest(MailboxByAccountIdSelector mbox) {
        this.mbox = mbox;
    }

    public MailboxByAccountIdSelector getMbox() { return mbox; }
}
