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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;

/**
 *  A {@link Session} is identified by an (accountId, sessionID) pair.  A single
 *  account may have multiple active sessions simultaneously.<p>
 *  
 *  In general, Sessions are created and managed by the {@link SessionCache} but
 *  this is not always the case.  Session subclasses  which are not stored in the 
 *  SessionCache must take special care to manage their own lifetimes (timeouts, 
 *  etc)
 */
public abstract class Session {
    protected final String    mAccountId;
    private   final String    mSessionId;
    private   final Type mSessionType;
    private   final IMPersona mPersona;

    protected Mailbox mMailbox;
    private   long    mLastAccessed;
    private   long    mCreationTime;
    private   boolean mCleanedUp = false;
    
    
    /**
     * Session Type
     */
    public enum Type {
        SOAP, IMAP, ADMIN, WIKI, 
        SYNCLISTENER, 
        WAITSET;
    }
    
    public Session(String accountId, String sessionId, Type type) throws ServiceException {
        mAccountId = accountId;
        mSessionId = sessionId;
        mSessionType = type;
        mCreationTime = System.currentTimeMillis();

        Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (acct != null && Provisioning.onLocalServer(acct)) {
            // add this Session to the Mailbox or die trying
            mMailbox = MailboxManager.getInstance().getMailboxByAccountId(accountId);
            mMailbox.addListener(this);

            // add this Session to the IM Persona, or die trying
            if (this.shouldRegisterWithIM()) {
                mPersona = IMRouter.getInstance().findPersona(null, mMailbox);
                synchronized(mPersona.getLock()) {
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

    public final void encodeState(Element parent) {
        doEncodeState(parent);
    }
    
    protected void doEncodeState(Element parent) { }

    /** Returns the Session's identifier. */
    public String getSessionId() { 
        return mSessionId;
    }

    /** Returns the type of the Session.
     * 
     * @see SessionCache#SESSION_ADMIN
     * @see SessionCache#SESSION_SOAP
     * @see SessionCache#SESSION_IMAP */
    public Session.Type getSessionType() {
        return mSessionType;
    }

    /** Returns the maximum idle duration (in milliseconds) for the Session.
     * 
     *  The idle lifetime must be CONSTANT 
     */
    protected abstract long getSessionIdleLifetime();

    /** Returns the {@link Mailbox} (if any) this Session is listening on. */
    public Mailbox getMailbox() {
        return mMailbox;
    }

    /** Returns whether the submitted account ID matches that of the Session's owner. */
    public boolean validateAccountId(String accountId) {
        return mAccountId.equals(accountId);
    }

    /** Handles the set of changes from a single Mailbox transaction.
     *  <p>
     *  Takes a set of new mailbox changes and caches it locally.  This is
     *  currently initiated from inside the Mailbox transaction commit, but we
     *  still shouldn't assume that execution of this method is synchronized
     *  on the Mailbox.
     *  <p>
     *  *All* changes are currently cached, regardless of the client's state/views.
     *
     * @param changeId The sync-token change Id of the change 
     * @param pms   A set of new change notifications from our Mailbox  */
    public abstract void notifyPendingChanges(int changeId, PendingModifications pns);
    
    /**
     * Notify this session that an IM event has occured
     * 
     * @param imn
     */
    public void notifyIM(IMNotification imn) {
        // do nothing by default.
    }
    
    /**
     * @return subclasses return TRUE if this session should be registered with IM
     *            ie, if the existence of this session should lead to an "online" presence
     *            in the IM system for this user.
     */
    protected boolean shouldRegisterWithIM() { 
        return false;
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
                synchronized (mPersona.getLock()) {
                    mPersona.removeListener(this);
                }
            }
        } finally {
            mCleanedUp = true;
        }
        mMailbox = null;
    }

    abstract protected void cleanup();
    
    public long getLastAccessTime() { 
        return mLastAccessed;
    }
    
    public long getCreationTime() { 
        return mCreationTime; 
    }

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
