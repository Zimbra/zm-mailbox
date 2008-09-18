/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import java.lang.ref.SoftReference;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.util.SyncToken;

/**
 * Simple struct used to define the parameters of an account during an add or update
 */
public class WaitSetAccount {
    public WaitSetAccount(String id, SyncToken sync, int interest) {
        this.accountId = id;
        this.lastKnownSyncToken = sync;
        this.interests = interest;
        this.ref = null;
    }
    public WaitSetSession getSession() {
        if (ref != null) {
            WaitSetSession toRet = ref.get();
            ZimbraLog.session.debug(toString()+" SoftReference has gone away, nulling it out");
            if (toRet == null)
                ref = null;
            return toRet;
        } else
            return null;
    }
    public String toString() {
        return "WaitSetAccount("+accountId+")";
    }
    
    public String accountId;
    public int interests;
    
    public SyncToken lastKnownSyncToken;
    
    public SoftReference<WaitSetSession> ref;
}