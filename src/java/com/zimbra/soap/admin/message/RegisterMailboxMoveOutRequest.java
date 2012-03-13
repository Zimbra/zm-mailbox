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

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.MailboxMoveSpec;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-description Register Mailbox move out.
 * <br />
 * This request is invoked by move destination server against move source server to signal the start of a mailbox
 * move.  The receiving server registers a move-out.  This helps prevent simultaneous moves of the same mailbox.
 * <br />
 * <br />
 * ALREADY_BEING_MOVED_OUT fault will be returned if mailbox is already in the middle of a move.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_REGISTER_MAILBOX_MOVE_OUT_REQUEST)
public class RegisterMailboxMoveOutRequest {

    /**
     * @zm-api-field-description Specification for Mailbox move
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=true)
    private MailboxMoveSpec account;

    private RegisterMailboxMoveOutRequest() {
    }

    private RegisterMailboxMoveOutRequest(MailboxMoveSpec account) {
        setAccount(account);
    }

    public static RegisterMailboxMoveOutRequest create(MailboxMoveSpec account) {
        return new RegisterMailboxMoveOutRequest(account);
    }

    public void setAccount(MailboxMoveSpec account) { this.account = account; }
    public MailboxMoveSpec getAccount() { return account; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("account", account);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
