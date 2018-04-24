/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.session;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

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
    private   final long   mCreationTime;

    private   String    mSessionId;
    protected volatile MailboxStore mailbox;
    private   long      mLastAccessed;
    private   boolean   mCleanedUp;
    private   boolean   mIsRegistered;
    private   boolean   mAddedToCache;
    protected int lastChangeId;
    protected String userAgent;
    protected String requestIPAddress;


    /**
     * Session Type
     */
    public enum Type {
        NULL(0, 0), // unused dummy session type
        SOAP(1, LC.zimbra_session_limit_soap.intValue()),
        IMAP(2, Math.max(0, LC.zimbra_session_limit_imap.intValue())),
        ADMIN(3, LC.zimbra_session_limit_admin.intValue()),
        WIKI(4, 0),
        SYNCLISTENER(5, LC.zimbra_session_limit_sync.intValue()),
        WAITSET(6, 0)
        ;

        Type(int index, int maxPerAccount) {
            mIndex = index;
            mMaxPerAccount = maxPerAccount;
        }

        private final int mIndex;
        private final int mMaxPerAccount;

        public final int getIndex() {
            return mIndex;
        }

        public final int getMaxPerAccount() {
            return mMaxPerAccount;
        }
    }

    /**
     * for non-Mailbox related notifications
     *
     */
    public abstract static class ExternalEventNotification {
        /** Add XML representation to the <notify/> block. */
        public abstract void addElement(Element notify);
        public boolean canAccess(Account account) {
            return true;
        }
    }

    /** Creates a {@code Session} of the given <tt>Type</tt> whose target
     *  {@link Account} is the same as its authenticated <tt>Account</tt>.
     * @param accountId  The account ID of the {@code Session}'s owner
     * @param type       The type of {@code Session} being created */
    public Session(String accountId, Type type) {
        this(accountId, accountId, type);
    }

    /** Creates a {@code Session} of the given <tt>Type</tt> with its owner
     *  and target specified separately.  In general, a {@code Session}
     *  should only be created on the server where the target mailbox lives.
     *
     * @param authId    The account ID of the {@code Session}'s owner
     * @param targetId  The account ID of the {@link Mailbox} the
     *                  {@code Session} is attached to
     * @param type      The type of {@code Session} being created */
    public Session(String authId, String targetId, Type type) {
        this(null, authId, targetId, type);
    }

    public Session(String sessionId, String authId, String targetId, Type type) {
        mAuthenticatedAccountId = authId;
        mTargetAccountId = targetId == null ? authId : targetId;
        mSessionType = type;
        mCreationTime = System.currentTimeMillis();
        mLastAccessed = mCreationTime;
        mSessionId = sessionId == null ? SessionCache.getNextSessionId(mSessionType) : sessionId;
    }

    public Type getType() {
        return mSessionType;
    }

    /** Registers the session as a listener on the target mailbox and adds
     *  it to the session cache.  When a session is added to the cache, its
     *  session ID is initialized.
     *
     * @see #isMailboxListener()
     * @see #isRegisteredInCache() */
    public Session register() throws ServiceException {
        if (mIsRegistered) {
            return this;
        }

        if (isMailboxListener()) {
            MailboxStore mbox = mailbox = MailboxManager.getInstance().getMailboxByAccountId(mTargetAccountId);

            // once addListener is called, you may NOT lock the mailbox (b/c of deadlock possibilities)
            if (mbox != null) {
                if (mbox instanceof Mailbox) {
                    ((Mailbox) mbox).getNotificationPubSub().getSubscriber().addListener(this);
                } else {
                    throw new UnsupportedOperationException(String.format(
                            "Session register only supports Mailbox currently can't handle %s",
                                    mbox.getClass().getName()));
                }
            }
        }

        // registering the session automatically sets mSessionId
        if (isRegisteredInCache()) {
            addToSessionCache();
        }

        mIsRegistered = true;

        return this;
    }

    /** added to avoid potential for stack overflow recursion with {@link #register()} */
    public Session register(Mailbox mbox) throws ServiceException {
        if (mIsRegistered) {
            return this;
        }

        if (isMailboxListener()) {
            mailbox = mbox;
            mbox.getNotificationPubSub().getSubscriber().addListener(this);
        }

        // registering the session automatically sets mSessionId
        if (isRegisteredInCache()) {
            addToSessionCache();
        }

        mIsRegistered = true;

        return this;
    }

    /** Unregisters the session as a listener on the target mailbox and removes
     *  it from the session cache.  When a session is removed from the cache,
     *  its session ID is nulled out.
     *
     * @see #isMailboxListener()
     * @see #isRegisteredInCache() */
    public Session unregister() {
        // locking order is always Mailbox then Session
        MailboxStore mboxStore = mailbox;
        if (null != mboxStore) {
            if (mboxStore instanceof Mailbox) {
                Mailbox mbox = (Mailbox)mboxStore;
                //assert(l.isWriteLockedByCurrentThread() || !Thread.holdsLock(this));
                if (isMailboxListener()) {
                    mbox.getNotificationPubSub().getSubscriber().removeListener(this);
                    mailbox = null;
                }
            } else if (isMailboxListener()) {
                throw new UnsupportedOperationException(String.format(
                        "Session unregister only supports Mailbox currently can't handle %s",
                                mboxStore.getClass().getName()));
            }
        }

        removeFromSessionCache();

        mIsRegistered = false;

        return this;
    }

    protected boolean isRegistered() {
        return mIsRegistered;
    }

    /** Whether the session should be attached to the target {@link Mailbox}
     *  when its {@link #register()} method is invoked. */
    abstract protected boolean isMailboxListener();

    /** Whether the session should be added to the {@link SessionCache} when
     *  its {@link #register()} method is invoked.  A session ID is assigned
     *  when a session is added to the cache. */
    abstract protected boolean isRegisteredInCache();

    public static final int OPERATION_HISTORY_LENGTH = 6;
    public static final int OPERATION_HISTORY_TIME = 10 * 1000;

    public final void encodeState(Element parent) {
        doEncodeState(parent);
    }

    protected void doEncodeState(Element parent) {}

    protected void addToSessionCache() {
        SessionCache.registerSession(this);
        mAddedToCache = true;
    }

    protected void removeFromSessionCache() {
        if (mAddedToCache) {
            SessionCache.unregisterSession(this);
            mAddedToCache = false;
        }
    }

    /** Returns whether this session has been added to the cache. */
    public boolean isAddedToSessionCache() {
        return mAddedToCache;
    }

    /** Returns the Session's identifier. */
    public String getSessionId() {
        return mSessionId;
    }

    /** Returns the Session's identifier, qualified uniquely by the server run. */
    public String getQualifiedSessionId() {
        return SessionCache.qualifySessionId(mSessionId);
    }

    /** Sets the Session's identifier. Used for unit testing only,
     * DO NOT CALL THIS API EXCEPT FOR TEST PURPOSES */
    final Session testSetSessionId(String sessionId) {
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
    public MailboxStore getMailbox() {
        return mailbox;
    }

    /** Most session types do not support MailboxStore types other than "Mailbox".
     * Calling this will ensure that an exception is thrown if the store is not a Mailbox (or null)
     * for any reason.
     * @return
     */
    public Mailbox getMailboxOrNull() {
        MailboxStore mboxStore = mailbox;
        if (null == mboxStore) {
            return null;
        }
        if (mboxStore instanceof Mailbox) {
            return (Mailbox) mboxStore;
        }
        throw new UnsupportedOperationException(String.format(
                "Operation not supported for non-Mailbox MailboxStore '%s'", mboxStore.getClass().getName()));
    }

    /** Handles the set of changes from a single Mailbox transaction.
     *  <p>
     *  Takes a set of new mailbox changes and caches it locally.  This is
     *  currently initiated from inside the Mailbox transaction commit, but we
     *  still shouldn't assume that execution of this method is synchronized
     *  on the Mailbox.
     *  <p>
     *  *All* changes are currently cached, regardless of the client's state/views.
     * @param pns                  A set of new change notifications from our Mailbox.
     * @param changeId             The change ID of the change.
     * @param sourceSessionInfo    Info about the {@code Session} originating these changes, or
     *                  <tt>null</tt> if none was specified. */
    public abstract void notifyPendingChanges(PendingModifications pns, int changeId, SourceSessionInfo sourceSessionInfo);

    /** Notify this session that an external event has occured. */
    public void notifyExternalEvent(ExternalEventNotification extra) {
        // do nothing by default.
    }

    /** Disconnects from any resources and deregisters as a {@link Mailbox}
     *  listener. */
    final void doCleanup() {
        if (mCleanedUp) {
            return;
        }

        try {
            cleanup();
            unregister();
        } finally {
            mCleanedUp = true;
        }
        mailbox = null;
    }

    abstract protected void cleanup();

    public long getLastAccessTime() {
        return mLastAccessed;
    }

    public long getCreationTime() {
        return mCreationTime;
    }

    /** Public API for updating the access time of a session. */
    public void updateAccessTime() {
        // go through the session cache so that the session cache's
        // time-ordered access list stays correct
        // see bug 16242
        SessionCache.lookup(mSessionId, mAuthenticatedAccountId);
    }

    /**
     * This API must only be called by the SessionCache,
     * all other callers should use updateAccessTime()
     */
    void sessionCacheSetLastAccessTime() {
        mLastAccessed = System.currentTimeMillis();
    }

    public boolean accessedAfter(long otherTime) {
        return mLastAccessed > otherTime;
    }

    /** Returns the account ID of the {@code Session}'s owner.
     * @see #getTargetAccountId() */
    public String getAuthenticatedAccountId() {
        return mAuthenticatedAccountId;
    }

    /** Returns the account ID of the {@link Mailbox} that the {@code Session}
     *  is attached to.
     * @see #getAuthenticatedAccountId() */
    public String getTargetAccountId() {
        return mTargetAccountId;
    }

    public boolean isDelegatedSession() {
        return !mAuthenticatedAccountId.equalsIgnoreCase(mTargetAccountId);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestIPAddress() {
        return requestIPAddress;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper.add("id", mSessionId)
            .add("authAcct", mAuthenticatedAccountId);
        if (!Objects.equal(mAuthenticatedAccountId, mTargetAccountId)) {
            helper.add("targetAcct", mTargetAccountId);
        }
        if (null == mailbox) {
            helper.add("mbox", mailbox);
        }
        helper.add("lastAccessed", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date(mLastAccessed)));
        if (mCleanedUp) {
            helper.add("cleanedUp", mCleanedUp);
        }
        if (!mIsRegistered) {
            helper.add("registered", mIsRegistered);
        }
        if (!mAddedToCache) {
            helper.add("addedToCache", mAddedToCache);
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    public SourceSessionInfo toSessionInfo() {
        return new SourceSessionInfo(this);
    }

    public static class SourceSessionInfo {
        private String sessionId;
        private String wsId = null;

        public SourceSessionInfo() {}

        public SourceSessionInfo(Session sourceSession) {
            this.sessionId = sourceSession.getSessionId();
            if (sourceSession instanceof SoapSession) {
                wsId = ((SoapSession) sourceSession).getCurWaitSetID();
            }
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getWaitSetId() {
            return wsId;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Session) {
                return sessionId.equals(((Session) other).getSessionId());
            } else if (other instanceof SourceSessionInfo) {
                return sessionId.equals(((SourceSessionInfo) other).getSessionId());
            } else {
                return false;
            }
        }
    }
}
