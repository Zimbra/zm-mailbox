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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
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

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMRouter;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.common.util.StringUtil;

public abstract class Session {
    protected final String    mAccountId;
    private   final String    mSessionId;
    private   final int       mSessionType;
    private   final IMPersona mPersona;

    protected Mailbox mMailbox;
    private   long    mLastAccessed;
    private   boolean mCleanedUp = false;

    /** Implementation of the Session interface */
    public Session(String accountId, String sessionId, int type) throws ServiceException {
        mAccountId = accountId;
        mSessionId = sessionId;
        mSessionType = type;

        Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (acct != null && Provisioning.onLocalServer(acct)) {
            // add this Session to the Mailbox or die trying
            mMailbox = MailboxManager.getInstance().getMailboxByAccountId(accountId);
            mMailbox.addListener(this);

            // add this Session to the IM Persona, or die trying
            if (this.shouldRegisterWithIM()) {
                synchronized(mMailbox) {
                    mPersona = IMRouter.getInstance().findPersona(null, mMailbox, false);
                    mPersona.addListener(this);
                }
            } else {
                mPersona = null;
            }
        } else {
            mMailbox = null;  mPersona = null;
        }

        updateAccessTime();
    }

    public static class RecentOperation {
        public long mTimestamp;
        public Class mOperationClass;
        public RecentOperation(long ts, Class oc) {
            mTimestamp = ts;
            mOperationClass = oc;
        }
    }
    
    public static final int OPERATION_HISTORY_LENGTH = 6;
    public static final int OPERATION_HISTORY_TIME = 10 * 1000;
    
    private List<RecentOperation> mRecentOperations = new LinkedList<RecentOperation>();
    
    synchronized public void logOperation(Operation op) {
        long now = System.currentTimeMillis();
        long cutoff = now - OPERATION_HISTORY_TIME;

        if (mRecentOperations.size() >= OPERATION_HISTORY_LENGTH) 
            mRecentOperations.remove(0);

        while (mRecentOperations.size() > 0 && mRecentOperations.get(0).mTimestamp < cutoff)
            mRecentOperations.remove(0);

        mRecentOperations.add(new RecentOperation(now, op.getClass()));
    }

    synchronized public List<RecentOperation> getRecentOperations() { return mRecentOperations; }
    
    public void dumpState(Writer w) {
        try {
            w.write(this.toString());
        } catch(IOException e) { e.printStackTrace(); };
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
    
    /**
     * @return TRUE if this session should be registered with IM
     */
    protected abstract boolean shouldRegisterWithIM();

    public static enum RegisterNotificationResult {
        NO_NOTIFY, // notifications are not available for this session
        SENT_DATA, // notifications were waiting, have been sent
        WAITING; // none waiting, go ahead and block 
    }

    /**
     * A push channel has come online
     *
     * @return TRUE if the channel should stay open (wait for more data) or FALSE if the channel 
     *          has been used (data was sent         
     * @param conn
     * @throws ServiceException 
     */
    public RegisterNotificationResult registerNotificationConnection(OzNotificationConnectionHandler conn)
    throws ServiceException { 
        return RegisterNotificationResult.NO_NOTIFY; 
    }

    /** Disconnects from any resources and deregisters as a {@link Mailbox} listener. */
    final void doCleanup() {
        if (mCleanedUp)
            return;

        try {
            cleanup();
            if (mMailbox != null)
                mMailbox.removeListener(this);
            if (mPersona != null) {
                synchronized (mMailbox) {
                    mPersona.removeListener(this);
                }
            }
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

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    public String toString() {
        String dateString = mDateFormat.format(new Date(mLastAccessed));
        return StringUtil.getSimpleClassName(this) + ": {sessionId: " + mSessionId +
            ", accountId: " + mAccountId + ", lastAccessed: " + dateString + "}";
    }
}
