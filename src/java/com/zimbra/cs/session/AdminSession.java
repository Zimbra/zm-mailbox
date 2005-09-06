/*
 * Created on Aug 31, 2005
 */
package com.zimbra.cs.session;

/** @author dkarp */
public class AdminSession extends Session {

    AdminSession(String accountId, String sessionId) {
        super(accountId, sessionId, SessionCache.SESSION_ADMIN);
    }

    public void notifyPendingChanges(PendingModifications pns) { }

    protected void cleanup() {
    }
}
