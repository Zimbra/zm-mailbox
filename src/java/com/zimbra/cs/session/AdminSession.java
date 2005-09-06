/*
 * Created on Aug 31, 2005
 */
package com.zimbra.cs.session;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Constants;

/** @author dkarp */
public class AdminSession extends Session {

    private static final long ADMIN_SESSION_TIMEOUT_MSEC = 10 * Constants.MILLIS_PER_MINUTE;

    AdminSession(String accountId, String sessionId) throws ServiceException {
        super(accountId, sessionId, SessionCache.SESSION_ADMIN);
    }

    protected long getSessionIdleLifetime() {
        return ADMIN_SESSION_TIMEOUT_MSEC;
    }

    public void notifyPendingChanges(PendingModifications pns) { }

    protected void cleanup() {
    }
}
