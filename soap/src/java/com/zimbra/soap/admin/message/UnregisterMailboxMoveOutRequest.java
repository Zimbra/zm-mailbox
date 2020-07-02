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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.MailboxMoveSpec;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description This request is invoked by move destination server against move source server to
 * indicate the completion of mailbox move.  This request is also invoked to reset the state after a mailbox move that
 * died unexpectedly, such as when the destination server crashed.
 * <br />
 * <br />
 * NO_SUCH_MOVE_OUT fault is returned if there is no move-out in progress.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_UNREGISTER_MAILBOX_MOVE_OUT_REQUEST)
public class UnregisterMailboxMoveOutRequest {

    /**
     * @zm-api-field-description Details of Mailbox Move
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=true)
    private MailboxMoveSpec account;

    private UnregisterMailboxMoveOutRequest() {
    }

    private UnregisterMailboxMoveOutRequest(MailboxMoveSpec account) {
        setAccount(account);
    }

    public static UnregisterMailboxMoveOutRequest create(MailboxMoveSpec account) {
        return new UnregisterMailboxMoveOutRequest(account);
    }

    public void setAccount(MailboxMoveSpec account) { this.account = account; }
    public MailboxMoveSpec getAccount() { return account; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("account", account);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
