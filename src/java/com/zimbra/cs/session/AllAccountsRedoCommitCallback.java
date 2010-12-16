/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.session;

import java.util.Set;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.RedoCommitCallback;

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
