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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
    protected final String mAuthenticatedAccountId;
    protected final String mTargetAccountId;
    private   final Type   mSessionType;

    private   String    mSessionId;
    protected Mailbox   mMailbox;
    private   IMPersona mPersona;
    private   long      mLastAccessed;
    private   long      mCreationTime;
    private   boolean   mCleanedUp = false;

    
    /**
     * Session Type
     */
    public enum Type {
        NULL(0, 0), // unused dummy session type
        SOAP(1, 5), 
        IMAP(2, 5), 
        ADMIN(3, 5), 
        WIKI(4, 0), 
        SYNCLISTENER(5, 2), 
        WAITSET(6, 0)
        ;
        
        Type(int index, int maxPerAccount) {
            mIndex = index;
            mMaxPerAccount = maxPerAccount;
        }
        
        private final int mIndex;
        private final int mMaxPerAccount;
        public final int getIndex() { return mIndex; }
        public final int getMaxPerAccount() { return mMaxPerAccount; } 
    }

    /** Creates a <tt>Session</tt> of the given <tt>Type</tt> whose target
     *  {@link Account} is the same as its authenticated <tt>Account</tt>.
     * @param accountId  The account ID of the <tt>Session</tt>'s owner
     * @param type       The type of <tt>Session</tt> being created */
    public Session(String accountId, Type type) {
        this(accountId, accountId, type);
    }

    /** Creates a <tt>Session</tt> of the given <tt>Type</tt> with its owner
     *  and target specified separately.  In general, a <tt>Session</tt>
     *  should only be created on the server where the target mailbox lives.
     *  
     * @param authId    The account ID of the <tt>Session</tt>'s owner
     * @param targetId  The account ID of the {@link Mailbox} the
     *                  <tt>Session</tt> is attached to
     * @param type      The type of <tt>Session</tt> being created */
    public Session(String authId, String targetId, Type type) {
        mAuthenticatedAccountId = authId;
        mTargetAccountId = targetId == null ? authId : targetId;
        mSessionType = type;
        mCreationTime = System.currentTimeMillis();

        updateAccessTime();
    }

    /** Registers the session as a listener on the target mailbox and adds
     *  it to the session cache.  When a session is added to the cache, its
     *  session ID is initialized.
     *  
     * @see #isMailboxListener()
     * @see #isRegisteredInCache() */
    public Session register() throws ServiceException {
        if (mSessionId != null)
            return this;

        if (isMailboxListener()) {
            mMailbox = MailboxManager.getInstance().getMailboxByAccountId(mTargetAccountId);
            mMailbox.addListener(this);

            if (shouldRegisterWithIM() && mAuthenticatedAccountId.equalsIgnoreCase(mTargetAccountId)) {
                mPersona = IMRouter.getInstance().findPersona(null, mMailbox);
                synchronized (mPersona.getLock()) {
                    mPersona.addListener(this);
                }
            }
        }

        // registering the session automatically sets mSessionId
        if (isRegisteredInCache())
            SessionCache.registerSession(this);

        return this;
    }

    /** Unregisters the session as a listener on the target mailbox and removes
     *  it from the session cache.  When a session is removed from the cache,
     *  its session ID is nulled out.
     *  
     * @see #isMailboxListener()
     * @see #isRegisteredInCache() */
    public Session unregister() {
        if (mMailbox != null && isMailboxListener()) {
            mMailbox.removeListener(this);
            mMailbox = null;
        }

        if (mPersona != null) {
            synchronized (mPersona.getLock()) {
                mPersona.removeListener(this);
            }
            mPersona = null;
        }

        if (mSessionId != null && isRegisteredInCache()) {
            SessionCache.unregisterSession(this);
            mSessionId = null;
        }

        return this;
    }

    /** Whether the session should be attached to the target {@link Mailbox}
     *  when its {@link #register()} method is invoked. */
    abstract protected boolean isMailboxListener();

    /** Whether the session should be added to the {@link SessionCache} when
     *  its {@link #register()} method is invoked.  A session ID is assigned
     *  when a session is added to the cache. */
    abstract protected boolean isRegisteredInCache();


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

    /** Sets the Session's identifier. */
    Session setSessionId(String sessionId) { 
        mSessionId = sessionId;
        return this;
    }

    /** Returns the type of the Session.
     * @see Type */
    public Session.Type getSessionType() {
        return mSessionType;
    }

    /** Returns the maximum idle duration (in milliseconds) for the Session.
     *  The idle lifetime must be CONSTANT  */
    protected abstract long getSessionIdleLifetime();

    /** Returns the {@link Mailbox} (if any) this Session is listening on. */
    public Mailbox getMailbox() {
        return mMailbox;
    }

    /** Returns whether the submitted account ID matches that of the Session's owner. */
    public boolean validateAccountId(String accountId) {
        return mAuthenticatedAccountId.equals(accountId);
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
    
    /** Notify this session that an IM event has occured. */
    public void notifyIM(IMNotification imn) {
        // do nothing by default.
    }
    
    /** Returns whether this session should be registered with IM (i.e. if the
     *  existence of this session should lead to an "online" presence in the
     *  IM system for this user. */
    protected boolean shouldRegisterWithIM() { 
        return false;
    }

    /** Disconnects from any resources and deregisters as a {@link Mailbox} listener. */
    final void doCleanup() {
        if (mCleanedUp)
            return;

        try {
            cleanup();
            unregister();
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

    /** Returns the account ID of the <tt>Session</tt>'s owner.
     * @see #getTargetAccountId() */
    public String getAuthenticatedAccountId() {
        return mAuthenticatedAccountId;
    }

    /** Returns the account ID of the {@link Mailbox} that the <tt>Session</tt>
     *  is attached to.
     * @see #getAuthenticatedAccountId() */
    public String getTargetAccountId() {
        return mTargetAccountId;
    }

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    @Override
    public String toString() {
        String dateString = mDateFormat.format(new Date(mLastAccessed));
        return StringUtil.getSimpleClassName(this) + ": {sessionId: " + mSessionId +
            ", accountId: " + mAuthenticatedAccountId + ", lastAccessed: " + dateString + "}";
    }
}
