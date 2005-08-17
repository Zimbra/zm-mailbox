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
