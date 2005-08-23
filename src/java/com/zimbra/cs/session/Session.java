/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Nov 29, 2004
 */
package com.zimbra.cs.session;

public abstract class Session {
    protected String mAccountId;
    private   Object mContextId;
    private   long   mLastAccessed;

    /**
     * Implementation of the Session interface
     */
    public Session(String accountId, Object contextId) {
        mAccountId = accountId;
        mContextId = contextId;
        updateAccessTime();
        SessionCache.getInstance().mapAccountToSession(accountId, this);
    }

    public Object getSessionId() { 
        return mContextId;
    }

    public boolean validateAccountId(String accountId) {
        return mAccountId.equals(accountId);
    }
    
    protected abstract void notifyPendingChanges(PendingModifications pns);

    private boolean mCleanedUp = false;

    protected void finalize() {
        doCleanup(); // just in case it hasn't happened yet...
    }
    
    final void doCleanup() {
        if (!mCleanedUp) {
            try {
                cleanup();
            } finally {
                mCleanedUp = true;
                SessionCache.getInstance().removeSessionFromAccountMap(mAccountId, this);
            }
        }
    }
    
    abstract protected void cleanup();

    public void updateAccessTime() {
        mLastAccessed = System.currentTimeMillis();
    }
    
    public boolean accessedAfter(long otherTime) {
        return (mLastAccessed > otherTime);
    }

    public String getAccountId() {
    	return mAccountId;
    }
}
