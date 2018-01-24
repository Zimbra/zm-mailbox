/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.session;

import java.util.Set;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.RedoCommitCallback;

/**
 * 
 * @deprecated AllAccountsWaitSet is being deprecated
 *
 */
public class AllAccountsRedoCommitCallback implements RedoCommitCallback {
    private final String accountId;
    private final Set<MailItem.Type> changeTypes;

    private AllAccountsRedoCommitCallback(String accountId, Set<MailItem.Type> types) {
        this.accountId = accountId;
        changeTypes = types;
    }

    @Override
    public void callback(CommitId cid) {
        AllAccountsWaitSet.mailboxChangeCommitted(cid.encodeToString(), accountId, changeTypes);
    }

    public static final AllAccountsRedoCommitCallback getRedoCallbackIfNecessary(String accountId,
            Set<MailItem.Type> types) {
        if (AllAccountsWaitSet.isCallbackNecessary(types)) {
            return new AllAccountsRedoCommitCallback(accountId, types);
        }
        return null;
    }

}
