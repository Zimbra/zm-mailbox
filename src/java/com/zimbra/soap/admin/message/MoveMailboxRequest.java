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
import com.zimbra.soap.admin.type.MoveMailboxInfo;

/**
 * @zm-api-command-description Move a mailbox
 * <br />
 * Note: This request should be sent to the move destination server, rather than the source server.
 * <br />
 * <br />
 * Moves the mailbox of the specified account to this host.
 * The src and dest attributes are required as safety checks.  src must be set to the current home server of the
 * account, and dest must be set to the server receiving the request.
 * <br />
 * <br />
 * <b>syncFinishThreshold</b> and <b>maxSyncs</b> determine the blob/index sync behavior while account remains
 * unlocked.  Unlocked sync phase is done if sync has been repeated maxSyncs times, or if the last sync time was less
 * than or equal to syncFinishThreshold milliseconds.  After the unlocked phase the account is locked to quiesce it,
 * then one final sync is done followed by database export/import.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_MOVE_MAILBOX_REQUEST)
public class MoveMailboxRequest {

    /**
     * @zm-api-field-description Specification for the account move
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=true)
    private MoveMailboxInfo account;

    private MoveMailboxRequest() {
    }

    private MoveMailboxRequest(MoveMailboxInfo account) {
        setAccount(account);
    }

    public static MoveMailboxRequest create(MoveMailboxInfo account) {
        return new MoveMailboxRequest(account);
    }

    public void setAccount(MoveMailboxInfo account) { this.account = account; }
    public MoveMailboxInfo getAccount() { return account; }

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
