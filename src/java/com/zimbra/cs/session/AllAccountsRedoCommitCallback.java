/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.RedoCommitCallback;

/**
 * 
 */
public class AllAccountsRedoCommitCallback implements RedoCommitCallback {
    
    private AllAccountsRedoCommitCallback(String accountId, int changeMask) {
        mAccountId = accountId;
        mChangeMask = changeMask;
    }

    /* @see com.zimbra.cs.redolog.RedoCommitCallback#callback(com.zimbra.cs.redolog.CommitId) */
    public void callback(CommitId cid) {
        AllAccountsWaitSet.mailboxChangeCommitted(cid.encodeToString(), mAccountId, mChangeMask);
    }
    
    public static final AllAccountsRedoCommitCallback getRedoCallbackIfNecessary(String accountId, int changeMask) {
        if (AllAccountsWaitSet.isCallbackNecessary(changeMask)) {
            return new AllAccountsRedoCommitCallback(accountId, changeMask);
        }
        return null;
    }
    
    private final String mAccountId;
    private final int mChangeMask;
}
