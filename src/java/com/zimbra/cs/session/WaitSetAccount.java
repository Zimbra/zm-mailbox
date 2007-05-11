package com.zimbra.cs.session;

import java.lang.ref.SoftReference;

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
            if (toRet == null)
                ref = null;
            return toRet;
        } else
            return null;
    }
    public String accountId;
    public int interests;
    
    public SyncToken lastKnownSyncToken;
    
    public SoftReference<WaitSetSession> ref;
}