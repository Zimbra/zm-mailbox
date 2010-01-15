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
