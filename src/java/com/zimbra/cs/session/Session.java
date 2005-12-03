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

/*
 * Created on Nov 29, 2004
 *
 * DO NOT INSTANTIATE THIS DIRECTLY -- instead call SessionCache.getNewSession() 
 * to create objects of this type.
 */
package com.zimbra.cs.session;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.im.IMNotification;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.StringUtil;

public abstract class Session {

    protected String  mAccountId;
    private   String  mSessionId;
    private   int     mSessionType;
    private   long    mLastAccessed;
    private   boolean mCleanedUp = false;
    private   Mailbox mMailbox;

    /** Implementation of the Session interface */
    public Session(String accountId, String sessionId, int type) throws ServiceException {
        mAccountId = accountId;
        mSessionId = sessionId;
        mSessionType = type;

        Account acct = Provisioning.getInstance().getAccountById(accountId);
        if (acct != null && acct.isCorrectHost()) {
            // add this Session to the Mailbox or die trying
            mMailbox = Mailbox.getMailboxByAccountId(accountId);
            mMailbox.addListener(this);
        }
        updateAccessTime();
    }

    /** Returns the Session's identifier. */
    public String getSessionId() { 
        return mSessionId;
    }

    /** Returns the type of the Session.
     * 
     * @see SessionCache#SESSION_ADMIN
     * @see SessionCache#SESSION_SOAP
     * @see SessionCache#SESSION_IMAP */
    public int getSessionType() {
        return mSessionType;
    }

    /** Returns the maximum idle duration (in milliseconds) for the Session. */
    protected abstract long getSessionIdleLifetime();

    /** Returns the {@link Mailbox} (if any) this Session is listening on. */
    public Mailbox getMailbox() {
        return mMailbox;
    }

    /** Returns whether the submitted account ID matches that of the Session's owner. */
    public boolean validateAccountId(String accountId) {
        return mAccountId.equals(accountId);
    }

    public abstract void notifyPendingChanges(PendingModifications pns);
    
    public abstract void notifyIM(IMNotification imn);

    protected void finalize() {
        doCleanup(); // just in case it hasn't happened yet...
    }

    /** Disconnects from any resources and deregisters as a {@link Mailbox} listener. */
    final void doCleanup() {
        if (mCleanedUp)
            return;

        try {
            cleanup();
            if (mMailbox != null)
                mMailbox.removeListener(this);
        } finally {
            mCleanedUp = true;
        }
        mMailbox = null;
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

    private SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    public String toString() {
        String dateString = sDateFormat.format(new Date(mLastAccessed));
        return StringUtil.getSimpleClassName(this) + ": {sessionId: " + mSessionId +
            ", accountId: " + mAccountId + ", lastAccessed: " + dateString + "}";
    }
}
