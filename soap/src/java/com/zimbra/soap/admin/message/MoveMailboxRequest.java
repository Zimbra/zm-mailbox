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
import com.zimbra.soap.admin.type.MoveMailboxInfo;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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
    @ZimbraUniqueElement
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
