/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 9, 2004
 */
package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContextData;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.mail.GetFolder;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ProxyTarget;
import com.zimbra.soap.ZimbraSoapContext;

public class SoapSession extends Session {
    public class DelegateSession extends Session {
        private long mNextFolderCheck;
        private Set<Integer> mVisibleFolderIds;

        DelegateSession(String authId, String targetId) {
            super(authId, targetId, Type.SOAP);
        }

        public SoapSession getParentSession() {
            return SoapSession.this;
        }

        @Override public DelegateSession register() {
            try {
                super.register();
                calculateVisibleFolders(true);
                return this;
            } catch (ServiceException e) {
                unregister();
                return null;
            }
        }

        @Override public DelegateSession unregister() {
            super.unregister();
            removeDelegateSession(this);
            return this;
        }

        @Override protected boolean isMailboxListener() {
            return true;
        }

        @Override protected boolean isRegisteredInCache() {
            return false;
        }

        @Override protected long getSessionIdleLifetime() {
            return Integer.MAX_VALUE;
        }

        @Override public void cleanup()  { }

        @Override public void notifyPendingChanges(PendingModifications pms, int changeId, Session source) {
            try {
                if (calculateVisibleFolders(false))
                    pms = filterNotifications(pms);
                if (pms != null && pms.hasNotifications())
                    handleNotifications(pms, source == this || source == SoapSession.this);
            } catch (ServiceException e) {
                ZimbraLog.session.warn("exception during delegated notifyPendingChanges", e);
            }
        }

        private boolean calculateVisibleFolders(boolean force) throws ServiceException {
            long now = System.currentTimeMillis();

            Mailbox mbox = mMailbox;
            if (mbox == null) {
                mVisibleFolderIds = Collections.emptySet();
                return true;
            }

            synchronized (mbox) {
                if (!force && mNextFolderCheck > now)
                    return mVisibleFolderIds != null;

                Set<Folder> visible = mbox.getVisibleFolders(new OperationContext(getAuthenticatedAccountId()));
                Set<Integer> ids = null;
                if (visible != null) {
                    ids = new HashSet<Integer>(visible.size());
                    for (Folder folder : visible)
                        ids.add(folder.getId());
                }
    
                mVisibleFolderIds = ids;
                mNextFolderCheck = now + SOAP_SESSION_TIMEOUT_MSEC / 2;
                return ids != null;
            }
        }

        private boolean folderRecalcRequired(PendingModifications pms) {
            boolean recalc = false;
            if (pms.created != null && !pms.created.isEmpty()) {
                for (MailItem item : pms.created.values()) {
                    if (item instanceof Folder)
                        return true;
                }
            }
            if (!recalc && pms.modified != null && !pms.modified.isEmpty()) {
                for (Change chg : pms.modified.values()) {
                    if ((chg.why & (Change.MODIFIED_ACL | Change.MODIFIED_FOLDER)) != 0 && chg.what instanceof Folder)
                        return true;
                }
            }
            return false;
        }

        private static final int BASIC_CONVERSATION_FLAGS = Change.MODIFIED_FLAGS | Change.MODIFIED_TAGS | Change.MODIFIED_UNREAD;
        private static final int MODIFIED_CONVERSATION_FLAGS = BASIC_CONVERSATION_FLAGS | Change.MODIFIED_SIZE  | Change.MODIFIED_SENDERS;

        private PendingModifications filterNotifications(PendingModifications pms) throws ServiceException {
            // first, recalc visible folders if any folders got created or moved or had their ACL changed
            if (folderRecalcRequired(pms) && !calculateVisibleFolders(true))
                return pms;
            Set<Integer> visible = mVisibleFolderIds;
            if (visible == null)
                return pms;

            PendingModifications filtered = new PendingModifications();
            filtered.changedTypes = pms.changedTypes;
            if (pms.deleted != null && !pms.deleted.isEmpty()) {
                filtered.recordDeleted(pms.deleted.keySet(), pms.changedTypes);
            }
            if (pms.created != null && !pms.created.isEmpty()) {
                for (MailItem item : pms.created.values()) {
                    if (item instanceof Conversation || visible.contains(item instanceof Folder ? item.getId() : item.getFolderId()))
                        filtered.recordCreated(item);
                }
            }
            if (pms.modified != null && !pms.modified.isEmpty()) {
                for (Change chg : pms.modified.values()) {
                    if (chg.what instanceof MailItem) {
                        MailItem item = (MailItem) chg.what;
                        boolean isVisible = visible.contains(item instanceof Folder ? item.getId() : item.getFolderId());
                        boolean moved = (chg.why & Change.MODIFIED_FOLDER) != 0;
                        if (item instanceof Conversation) {
                            filtered.recordModified(item, chg.why | MODIFIED_CONVERSATION_FLAGS);
                        } else if (isVisible) {
                            filtered.recordModified(item, chg.why);
                            // if it's an unmoved visible message and it had a tag/flag/unread change, make sure the conv shows up in the modified or created list
                            if (item instanceof Message && (moved || (chg.why & BASIC_CONVERSATION_FLAGS) != 0))
                                forceConversationModification((Message) item, pms, filtered, moved ? MODIFIED_CONVERSATION_FLAGS : BASIC_CONVERSATION_FLAGS);
                        } else if (moved) {
                            filtered.recordDeleted(item);
                            // if it's a message and it's moved, make sure the conv shows up in the modified or created list
                            if (item instanceof Message)
                                forceConversationModification((Message) item, pms, filtered, MODIFIED_CONVERSATION_FLAGS);
                        }
                    } else if (chg.what instanceof Mailbox) {
                        if (((Mailbox) chg.what).hasFullAccess(new OperationContext(getAuthenticatedAccountId()))) {
                            filtered.recordModified((Mailbox) chg.what, chg.why);
                        }
                    }
                }
            }

            return filtered;
        }

        private void forceConversationModification(Message msg, PendingModifications pms, PendingModifications filtered, int changeMask) {
            int convId = msg.getConversationId();
            Mailbox mbox = msg.getMailbox();
            ModificationKey mkey = new ModificationKey(mbox.getAccountId(), convId);
            Change existing = null;
            if (pms.created != null && pms.created.containsKey(mkey)) {
                ;
            } else if (pms.modified != null && (existing = pms.modified.get(mkey)) != null) {
                filtered.recordModified((MailItem) existing.what, existing.why | changeMask);
            } else {
                try {
                    filtered.recordModified(mbox.getConversationById(null, convId), changeMask);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);
                } catch (Throwable t) { }
            }
        }
    }

    private class RemoteSessionInfo {
        final String mServerId, mSessionId;
        final long mLastRequest;
        long mLastFailedPing;

        RemoteSessionInfo(String sessionId, String serverId, long lastPoll) {
            mSessionId = sessionId;  mServerId = serverId;  mLastRequest = lastPoll;
        }
    }

    private static class RemoteNotifications {
        int count = -1;
        String deleted;
        List<Element> created;
        List<Element> modified;

        RemoteNotifications(Element eNotify) {
            if (eNotify == null)
                return;
            Element eSection;
            if ((eSection = eNotify.getOptionalElement(ZimbraNamespace.E_DELETED)) != null)
                deleted = eSection.getAttribute(A_ID, null);
            if ((eSection = eNotify.getOptionalElement(ZimbraNamespace.E_CREATED)) != null)
                created = new ArrayList<Element>(eSection.listElements());
            if ((eSection = eNotify.getOptionalElement(ZimbraNamespace.E_MODIFIED)) != null)
                modified = new ArrayList<Element>(eSection.listElements());
        }

        RemoteNotifications add(RemoteNotifications rns) {
            if (rns == null)
                return this;

            // FIXME: need to switch to the "delete I wipes create/modify I" model
            if (deleted == null)           deleted = rns.deleted;
            else if (rns.deleted != null)  deleted = deleted + "," + rns.deleted;

            if (created == null)           created = rns.created;
            else if (rns.created != null)  created.addAll(rns.created);

            if (modified == null)           modified = rns.modified;
            else if (rns.modified != null)  modified.addAll(rns.modified);

            count = (count >= 0 && rns.count >= 0 ? count + rns.count : -1);
            return this;
        }

        int getScaledNotificationCount() {
            if (count == -1) {
                count = 0;
                if (deleted != null)   count += StringUtil.countOccurrences(deleted, ',') / 4 + 1;
                if (created != null)   count += created.size();
                if (modified != null)  count += modified.size();
            }
            return count;
        }

        boolean hasNotifications() {
            if (deleted != null && !deleted.equals(""))   return true;
            if (created != null && !created.isEmpty())    return true;
            if (modified != null && !modified.isEmpty())  return true;
            return false;
        }
    }

    class QueuedNotifications {
        /** IMNotifications are strictly sequential right now */
        List<IMNotification> mIMNotifications;
        PendingModifications mMailboxChanges;
        RemoteNotifications mRemoteChanges;
        boolean mHasLocalChanges;

        /** used by the Session object to ensure that notifications are reliably
         *  received by the listener */
        private int mSequence;
        int getSequence()  { return mSequence; }

        QueuedNotifications(int seqno)  { mSequence = seqno; }

        boolean hasNotifications() {
            return hasNotifications(false);
        }

        boolean hasNotifications(boolean localMailboxOnly) {
            if (localMailboxOnly ? mHasLocalChanges : mMailboxChanges != null && mMailboxChanges.hasNotifications())
                return true;
            if (!localMailboxOnly && mRemoteChanges != null && mRemoteChanges.hasNotifications())
                return true;
            if (mIMNotifications != null && !mIMNotifications.isEmpty())
                return true;
            return false;
        }

        int getScaledNotificationCount() {
            return (mMailboxChanges == null ? 0 : mMailboxChanges.getScaledNotificationCount()) +
                   (mRemoteChanges == null  ? 0 : mRemoteChanges.getScaledNotificationCount());
        }

        void addNotification(IMNotification imn) {
            if (mIMNotifications == null) 
                mIMNotifications = new LinkedList<IMNotification>();
            mIMNotifications.add(imn);
        }

        void addNotification(PendingModifications pms) {
            if (pms == null || !pms.hasNotifications())
                return;
            if (mMailboxChanges == null) 
                mMailboxChanges = new PendingModifications();
            mMailboxChanges.add(pms);
            if (!mHasLocalChanges)
                mHasLocalChanges |= pms.overlapsWithAccount(mAuthenticatedAccountId);
        }

        void addNotification(RemoteNotifications rns) {
            if (mRemoteChanges == null)
                mRemoteChanges = rns;
            else
                mRemoteChanges.add(rns);
        }

        void clearMailboxChanges() {
            mMailboxChanges = null;
            mRemoteChanges = null;
            // note that mHasLocalChanges does *not* get reset when we trigger a <refresh> condition... 
        }
    }

    // Read/write access to all these members requires synchronizing on "this".
    private String mQueryStr = "";
    private String mGroupBy  = "";
    private String mSortBy   = "";
    private ZimbraQueryResults mQueryResults;

    private int  mRecentMessages;
    private long mPreviousAccess = -1;
    private long mLastWrite      = -1;

    // read/write access to all these members requires synchronizing on "mSentChanges"
    private   int mForceRefresh;
    protected LinkedList<QueuedNotifications> mSentChanges = new LinkedList<QueuedNotifications>();
    protected QueuedNotifications mChanges = new QueuedNotifications(1);

    private PushChannel mPushChannel = null;

    private boolean mUnregistered;
    private Map<String, DelegateSession> mDelegateSessions = new HashMap<String, DelegateSession>(3);
    private List<RemoteSessionInfo> mRemoteSessions;

    static final long SOAP_SESSION_TIMEOUT_MSEC = Math.max(5, LC.zimbra_session_timeout_soap.intValue()) * Constants.MILLIS_PER_SECOND;

    // if a keepalive request to a remote session failed, how long to wait before a new ping is permitted
    private static final long MINIMUM_PING_RETRY_TIME = 30 * Constants.MILLIS_PER_SECOND;

    private static final int MAX_QUEUED_NOTIFICATIONS = LC.zimbra_session_max_pending_notifications.intValue();


    /** Creates a <tt>SoapSession</tt> owned by the given account and
     *  listening on its {@link Mailbox}.
     * @see Session#register() */
    public SoapSession(String authenticatedId) {
        super(authenticatedId, Session.Type.SOAP);
    }

    @Override public SoapSession register() throws ServiceException {
        super.register();

        Mailbox mbox = mMailbox;
        if (mbox != null) {
            mRecentMessages = mbox.getRecentMessageCount();
            mPreviousAccess = mbox.getLastSoapAccessTime();
            mUnregistered = false;

            if (ZimbraLog.session.isDebugEnabled())
                ZimbraLog.session.debug("initializing session recent count to " + mRecentMessages);
        }
        return this;
    }

    @Override public SoapSession unregister() {
        // when the session goes away, record the timestamp of the last write op to the database
        Mailbox mbox = mMailbox;
        if (mLastWrite != -1 && mbox != null) {
            try {
                mbox.recordLastSoapAccessTime(mLastWrite);
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.session.warn("exception recording unloaded session's last access time", t);
            }
        }

        // unloading a SoapSession also must unload all its delegates
        List<DelegateSession> delegates;
        synchronized (mDelegateSessions) {
            delegates = new ArrayList<DelegateSession>(mDelegateSessions.values());
            mDelegateSessions.clear();
            mUnregistered = true;
        }
        for (DelegateSession ds : delegates)
            ds.unregister();

        // note that Session.unregister() unsets mMailbox...
        super.unregister();
        return this;
    }

    @Override protected boolean isMailboxListener() {
        return true;
    }

    @Override protected boolean isRegisteredInCache() {
        return true;
    }

    @Override protected boolean isIMListener() {
        return true;
//        try {
//            return Provisioning.getInstance().get(AccountBy.id, this.getTargetAccountId()).getBooleanAttr(Provisioning.A_zimbraPrefIMAutoLogin, false);
//        } catch (ServiceException e) {
//            ZimbraLog.session.info("Caught exception fetching account preference A_zimbraPrefIMAutoLogin", e);
//            return false;
//        }
    }

    @Override protected long getSessionIdleLifetime() {
        return SOAP_SESSION_TIMEOUT_MSEC;
    }

    private boolean mIsOffline = false;
    
    public boolean isOfflineSoapSession()  { return mIsOffline; }
    public void setOfflineSoapSession()    { mIsOffline = true; }


    public Session getDelegateSession(String targetAccountId) {
        if (mUnregistered || targetAccountId == null)
            return null;

        // delegate sessions are only for mailboxes on the local host
        try {
            if (!Provisioning.onLocalServer(Provisioning.getInstance().get(Provisioning.AccountBy.id, targetAccountId)))
                return null;
        } catch (ServiceException e) {
            ZimbraLog.session.info("exception while fetching delegate session", e);
            return null;
        }

        // catch the case where they ask for a delegate session for the same user
        targetAccountId = targetAccountId.toLowerCase();
        if (targetAccountId.equalsIgnoreCase(mAuthenticatedAccountId))
            return this;

        synchronized (mDelegateSessions) {
            // don't return delegate sessions after an unregister()
            if (mUnregistered)
                return null;

            DelegateSession ds = mDelegateSessions.get(targetAccountId);
            if (ds == null) {
                ds = new DelegateSession(mAuthenticatedAccountId, targetAccountId).register();
                if (ds != null)
                    mDelegateSessions.put(targetAccountId, ds);
            }
            return ds;
        }
    }

    void removeDelegateSession(DelegateSession ds) {
        synchronized (mDelegateSessions) {
            if (mUnregistered || mDelegateSessions.isEmpty())
                return;
            boolean removed = mDelegateSessions.remove(ds.mTargetAccountId.toLowerCase()) != null;
            if (!removed)
                return;
        }
        synchronized (mSentChanges) {
            mForceRefresh = mChanges.getSequence();
        }
    }


    public String getRemoteSessionId(Server server) {
        synchronized (this) {
            if (mMailbox == null || mRemoteSessions == null || server == null)
                return null;
            for (RemoteSessionInfo rsi : mRemoteSessions)
                if (rsi.mServerId.equals(server.getId()))
                    return rsi.mSessionId;
            return null;
        }
    }

    private boolean registerRemoteSessionId(Server server, String sessionId) {
        if (mMailbox == null || server == null || sessionId == null)
            return true;

        String serverId = server.getId().toLowerCase();
        synchronized (this) {
            boolean isNewEntry = true;
            if (mRemoteSessions == null) {
                mRemoteSessions = new LinkedList<RemoteSessionInfo>();
            } else {
                for (Iterator<RemoteSessionInfo> it = mRemoteSessions.iterator(); it.hasNext(); ) {
                    if (it.next().mServerId.equals(server.getId())) {
                        it.remove();
                        isNewEntry = false;
                    }
                }
            }
            mRemoteSessions.add(new RemoteSessionInfo(sessionId, serverId, System.currentTimeMillis()));
            return isNewEntry;
        }
    }

    public void handleRemoteNotifications(Server server, Element context) {
        handleRemoteNotifications(server, context, false, false);
    }

    private void handleRemoteNotifications(Server server, Element context, boolean ignoreRefresh, boolean isPing) {
        if (context == null)
            return;

        boolean refreshExpected = true;

        // remember the remote session ID for the server
        Element eSession = context.getOptionalElement(HeaderConstants.E_SESSION);
        boolean isSoap = eSession != null && eSession.getAttribute(HeaderConstants.A_TYPE, null) == null;
        String sessionId = eSession == null ? null : eSession.getAttribute(HeaderConstants.A_ID, null);
        if (isSoap && sessionId != null && !sessionId.equals(""))
            refreshExpected = registerRemoteSessionId(server, sessionId);

        // remote refresh should cause overall refresh
        if (!ignoreRefresh && !refreshExpected && context.getOptionalElement(ZimbraNamespace.E_REFRESH) != null)
            mForceRefresh = getCurrentNotificationSequence();

        Element eNotify = context.getOptionalElement(ZimbraNamespace.E_NOTIFY);
        if (eNotify != null) {
            RemoteNotifications rns = new RemoteNotifications(eNotify);
            synchronized (mSentChanges) {
                if (!skipNotifications(rns.getScaledNotificationCount(), !isPing))
                    mChanges.addNotification(rns);
            }
        }
    }

    private void pingRemoteSessions(ZimbraSoapContext zsc) {
        long now = System.currentTimeMillis();

        List<RemoteSessionInfo> needsPing = null;
        synchronized (this) {
            if (mRemoteSessions == null)
                return;

            long cutoff = now - getSessionIdleLifetime() / 2;
            for (RemoteSessionInfo rsi : mRemoteSessions) {
                if (rsi.mLastRequest < cutoff && now - rsi.mLastFailedPing > MINIMUM_PING_RETRY_TIME) {
                    if (needsPing == null)
                        needsPing = new LinkedList<RemoteSessionInfo>();
                    needsPing.add(rsi);
                }
            }
        }

        if (needsPing == null)
            return;

        Provisioning prov = Provisioning.getInstance();
        for (RemoteSessionInfo rsi : needsPing) {
            try {
                Element noop = Element.create(zsc.getRequestProtocol(), MailConstants.NO_OP_REQUEST);
                Server server = prov.getServerById(rsi.mServerId);
    
                ZimbraSoapContext zscProxy = new ZimbraSoapContext(zsc, mAuthenticatedAccountId);
                zscProxy.setProxySession(rsi.mSessionId);
    
                ProxyTarget proxy = new ProxyTarget(server, zscProxy.getAuthToken(), URLUtil.getSoapURL(server, false));
                proxy.disableRetries().setTimeouts(10 * Constants.MILLIS_PER_SECOND);
                Pair<Element, Element> envelope = proxy.execute(noop.detach(), zscProxy);
                handleRemoteNotifications(server, envelope.getFirst(), true, true);
            } catch (ServiceException e) {
                rsi.mLastFailedPing = now;
            }
        }
    }


    /** Returns the number of messages added to the Mailbox since the time
     *  returned by {@link #getPreviousSessionTime()}.  Note that this means
     *  that messages added to the Mailbox during this session are included
     *  in the count. */
    public int getRecentMessageCount() {
        return mRecentMessages;
    }

    /** Returns the time (in milliseconds) of last write op from any SOAP
     *  session <u>before</u> this session was initiated.  This value remains
     *  constant for the duration of this session. */
    public long getPreviousSessionTime() { 
        return mPreviousAccess;
    }

    /** Returns the time (in milliseconds) of the last write operation
     *  initiated by this session.  If the session has not done any write ops
     *  yet, returns {@link #getPreviousSessionTime()}. */
    public long getLastWriteAccessTime() {
        return mLastWrite == -1 ? mPreviousAccess : mLastWrite;
    }


    @Override public void doEncodeState(Element parent) {
        if (mPushChannel != null)
            parent.addAttribute("push", true);
    }


    /** A callback interface which is listening on this session and waiting
     *  for new notifications */
    public static interface PushChannel {
        public void closePushChannel();
        public int getLastKnownSequence();
        public ZimbraSoapContext getSoapContext();
        public boolean localChangesOnly();
        public void notificationsReady() throws ServiceException; 
    }

    public static enum RegisterNotificationResult {
        NO_NOTIFY,      // notifications not available for this session
        DATA_READY,     // notifications already here
        BLOCKING;       // none here yet, wait
    }

    /** Record that a push channel has come online.
     *
     * @param sc                The push channel. 
     * @param includeDelegates  Whether notifications from delegate sessions
     *                          trigger {@link RegisterNotificationResult#DATA_READY}.
     * @return the state of the channel (@see RegisterNotificationResult} */
    public RegisterNotificationResult registerNotificationConnection(final PushChannel sc)
    throws ServiceException {
        // don't have to lock the Mailbox before locking the Session to avoid deadlock because we're not calling any ToXML functions
        synchronized (this) {
            if (mPushChannel != null) {
                mPushChannel.closePushChannel();
                mPushChannel = null;
            }

            if (mMailbox == null) {
                sc.closePushChannel();
                return RegisterNotificationResult.NO_NOTIFY;
            }

            boolean dataReady;
            synchronized (mSentChanges) {
                // are there any notifications already pending given the passed-in seqno?
                int lastSeqNo = sc.getLastKnownSequence();
                dataReady = mChanges.hasNotifications(sc.localChangesOnly());
                if (!dataReady && mChanges.getSequence() > lastSeqNo + 1 && !mSentChanges.isEmpty()) {
                    for (QueuedNotifications ntfn : mSentChanges) {
                        if (ntfn.getSequence() > lastSeqNo && ntfn.hasNotifications(sc.localChangesOnly())) {
                            dataReady = true;  break;
                        }
                    }
                }
            }
            if (dataReady) {
                sc.notificationsReady();  
                return RegisterNotificationResult.DATA_READY;
            } else {
                mPushChannel = sc;
                return RegisterNotificationResult.BLOCKING;
            }
        }
    }


    @Override public void notifyIM(IMNotification imn) {
        if (imn == null)
            return;

        synchronized (mSentChanges) {
            mChanges.addNotification(imn);
        }
        try {
            // if we're in a hanging no-op, alert the client that there are changes
            notifyPushChannel(null, true);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in notifyIM", e);
        }
    }
    
    /** On the session's first write op, record the timestamp to the database. */
    public void updateLastWrite(Mailbox mbox) {
        boolean firstWrite = mLastWrite == -1;
        mLastWrite = System.currentTimeMillis();
        if (firstWrite) {
            try {
                mbox.recordLastSoapAccessTime(mLastWrite);
            } catch (ServiceException e) {
                ZimbraLog.session.warn("ServiceException in notifyPendingChanges ", e);
            }
        }
    }

    /** Handles the set of changes from a single Mailbox transaction.
     *  <p>
     *  Takes a set of new mailbox changes and caches it locally.  This is
     *  currently initiated from inside the Mailbox transaction commit, but we
     *  still shouldn't assume that execution of this method is synchronized
     *  on the Mailbox.
     *  <p>
     *  *All* changes are currently cached, regardless of the client's state/views.
     * @param pms       A set of new change notifications from our Mailbox.
     * @param changeId  The sync-token change id of the change.
     * @param source    The (optional) Session which initiated these changes. */
    @Override public void notifyPendingChanges(PendingModifications pms, int changeId, Session source) {
        Mailbox mbox = mMailbox;
        if (pms == null || mbox == null || !pms.hasNotifications())
            return;

        if (source == this) {
            updateLastWrite(mbox);
        } else {
            // keep track of "recent" message count: all present before the session started, plus all received during the session
            if (pms.created != null) {
                for (MailItem item : pms.created.values()) {
                    if (item instanceof Message) {
                        boolean isReceived = true;
                        if (item.getFolderId() == Mailbox.ID_FOLDER_SPAM || item.getFolderId() == Mailbox.ID_FOLDER_TRASH)
                            isReceived = false;
                        else if ((item.getFlagBitmask() & Mailbox.NON_DELIVERY_FLAGS) != 0)
                            isReceived = false;
                        else if (source != null)
                            isReceived = false;

                        if (isReceived) {
                            mRecentMessages++;
                            if (ZimbraLog.session.isDebugEnabled())
                                ZimbraLog.session.debug("incrementing session recent count to " + mRecentMessages);
                        }
                    }
                }
            }
        }

        handleNotifications(pms, source == this);
    }

    boolean hasSerializableChanges(PendingModifications pms) {
        // catch cases where the only notifications are mailbox config changes, which we don't serialize
        if (pms.created != null && !pms.created.isEmpty())
            return true;
        if (pms.deleted != null && !pms.deleted.isEmpty())
            return true;
        if (pms.modified != null && !pms.modified.isEmpty()) {
            for (Change chg : pms.modified.values()) {
                if (!(chg.what instanceof Mailbox) || chg.why != Change.MODIFIED_CONFIG)
                    return true;
            }
        }
        return false;
    }

    void handleNotifications(PendingModifications pms, boolean fromThisSession) {
        if (!hasSerializableChanges(pms))
            return;

        try {
            // update the set of notifications not yet sent to the client
            cacheNotifications(pms, fromThisSession);
            // if we're in a hanging no-op, alert the client that there are changes
            notifyPushChannel(pms, true);
            // FIXME: this query result cache purge seems a little aggressive
            clearCachedQueryResults();
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in notifyPendingChanges ", e);
        }
    }

    private void cacheNotifications(PendingModifications pms, boolean fromThisSession) {
        // XXX: should constrain to folders, tags, and stuff relevant to the current query?

        synchronized (mSentChanges) {
            if (!skipNotifications(pms.getScaledNotificationCount(), fromThisSession)) {
                // if we're here, these changes either
                //   a) do not cause the session's notification cache to overflow, or
                //   b) originate from this session and hence must be notified back to the session
                mChanges.addNotification(pms);
            }
        }
    }

    private boolean skipNotifications(int notificationCount, boolean fromThisSession) {
        // if we're going to be sending a <refresh> anyway, there's no need to record these changes
        int currentSequence = getCurrentNotificationSequence();
        if (mForceRefresh == currentSequence && !fromThisSession)
            return true;

        // determine whether this set of notifications would cause the cached set to overflow
        if (mForceRefresh != currentSequence && MAX_QUEUED_NOTIFICATIONS > 0) {
            // XXX: more accurate would be to combine pms and mChanges and take the count...
            int count = notificationCount + mChanges.getScaledNotificationCount();
            if (count > MAX_QUEUED_NOTIFICATIONS) {
                // if we've overflowed, jettison the pending change set
                mChanges.clearMailboxChanges();
                mForceRefresh = currentSequence;
            }

            for (QueuedNotifications ntfn : mSentChanges) {
                count += ntfn.getScaledNotificationCount();
                if (count > MAX_QUEUED_NOTIFICATIONS) {
                    ntfn.clearMailboxChanges();
                    mForceRefresh = Math.max(mForceRefresh, ntfn.getSequence());
                }
            }
        }

        return (mForceRefresh == currentSequence && !fromThisSession);
    }

    public void forcePush() {
        try {
            notifyPushChannel(null, false);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in forcePush", e);
        }
    }

    private void notifyPushChannel(final PendingModifications pms, final boolean clearChannel) throws ServiceException {
        // don't have to lock the Mailbox before locking the Session to avoid deadlock because we're not calling any ToXML functions
        synchronized (this) {
            if (mPushChannel == null)
                return;
            // ignore the notification if we're only interested in local changes and these aren't local
            if (mPushChannel.localChangesOnly() && pms != null && !pms.overlapsWithAccount(mAuthenticatedAccountId))
                return;

            mPushChannel.notificationsReady();
            if (clearChannel)
            	mPushChannel = null;
        }
    }


    public boolean requiresRefresh(final int lastSequence) {
        synchronized (mSentChanges) {
            if (lastSequence <= 0)
                return mForceRefresh == getCurrentNotificationSequence();
            else
                return mForceRefresh > Math.min(lastSequence, getCurrentNotificationSequence());
        }
    }

    /** Serializes basic folder/tag structure to a SOAP response header.
     *  <p>
     *  Adds a &lt;refresh> block to the existing &lt;context> element.
     *  This &lt;refresh> block contains the basic folder, tag, and mailbox
     *  size information needed to display and update the web UI's overview
     *  pane.  The &lt;refresh> block is sent when a new session is created.
     *  
     *  This API implicitly clears all cached notifications and therefore 
     *  should only been used during session creation.
     * @param ctxt  An existing SOAP header <context> element 
     * @param zsc   The SOAP request's encapsulated context */
    public void putRefresh(Element ctxt, ZimbraSoapContext zsc) throws ServiceException {
        Mailbox mbox = mMailbox;
        if (mbox == null)
            return;

        synchronized (mSentChanges) {
            for (QueuedNotifications ntfn : mSentChanges)
                ntfn.clearMailboxChanges();
        }

        Element eRefresh = ctxt.addUniqueElement(ZimbraNamespace.E_REFRESH);
        eRefresh.addAttribute(AccountConstants.E_VERSION, BuildInfo.FULL_VERSION, Element.Disposition.CONTENT);

        OperationContext octxt = DocumentHandler.getOperationContext(zsc, this);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        // dump current mailbox status (currently just size)
        ToXML.encodeMailbox(eRefresh, octxt, mbox);

        // dump all tags under a single <tags> parent
        List<Tag> tags = mbox.getTagList(octxt);
        if (tags != null && tags.size() > 0) {
            Element eTags = eRefresh.addUniqueElement(ZimbraNamespace.E_TAGS);
            for (Tag tag : tags) {
                if (tag != null && !(tag instanceof Flag))
                    ToXML.encodeTag(eTags, ifmt, tag);
            }
        }

        // first, get the user's folder hierarchy
        FolderNode root = mbox.getFolderTree(octxt, null, false);
        OperationContextData.setNeedGranteeName(octxt, false);
        GetFolder.encodeFolderNode(ifmt, octxt, eRefresh, root);

        Map<ItemId, Element> mountpoints = new HashMap<ItemId, Element>();
        // for mountpoints pointing to this host, get the serialized folder subhierarchy
        expandLocalMountpoints(octxt, root, eRefresh.getFactory(), mountpoints);
        // for mountpoints pointing to other hosts, get the folder structure from the remote server
        expandRemoteMountpoints(octxt, zsc, eRefresh.getFactory(), mountpoints);

        // graft in subfolder trees from the other user's mailbox, making sure that mountpoints reflect the counts (etc.) of the target folder
        if (!mountpoints.isEmpty())
            transferMountpointContents(eRefresh.getOptionalElement(MailConstants.E_FOLDER), octxt, mountpoints);
    }

    private void expandLocalMountpoints(OperationContext octxt, FolderNode node, Element.ElementFactory factory, Map<ItemId, Element> mountpoints) {
        if (node.mFolder == null || mountpoints == null) {
            return;
        } else if (node.mFolder instanceof Mountpoint) {
            Mountpoint mpt = (Mountpoint) node.mFolder;
            expandLocalMountpoint(octxt, mpt, factory, mountpoints);
        } else {
            for (FolderNode child : node.mSubfolders)
                expandLocalMountpoints(octxt, child, factory, mountpoints);
        }
    }

    private void expandLocalMountpoint(OperationContext octxt, Mountpoint mpt, Element.ElementFactory factory, Map<ItemId, Element> mountpoints) {
        // don't bother generating the subhierarchy more than once
        ItemId iidTarget = mpt.getTarget();
        if (mountpoints.containsKey(iidTarget))
            return;
        
        try {
            Provisioning prov = Provisioning.getInstance();
            Account owner = prov.get(Provisioning.AccountBy.id, mpt.getOwnerId(), octxt.getAuthToken());
            if (owner == null || owner.getId().equals(mAuthenticatedAccountId))
                return;

            // handle mountpoints pointing to a different server later
            if (!Provisioning.onLocalServer(owner)) {
                mountpoints.put(iidTarget, null);
                return;
            }

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);
            FolderNode remote = mbox.getFolderTree(octxt, new ItemId(mbox, mpt.getRemoteId()), false);
            
            if (remote != null && remote.mFolder != null && !remote.mFolder.isHidden()) {
                ItemIdFormatter ifmt = new ItemIdFormatter(octxt.getAuthenticatedUser(), mbox, false);
                if (OperationContextData.getNeedGranteeName(octxt))
                    OperationContextData.addGranteeNames(octxt, remote);
                Element subhierarchy = GetFolder.encodeFolderNode(ifmt, octxt, factory.createElement("ignored"), remote).detach();
                mountpoints.put(iidTarget, subhierarchy);
                // fault in a delegate session because there's actually something to listen on...
                getDelegateSession(mpt.getOwnerId());
            }
        } catch (ServiceException e) {
            return;
        }
    }

    private void expandRemoteMountpoints(OperationContext octxt, ZimbraSoapContext zsc, Element.ElementFactory factory, Map<ItemId, Element> mountpoints) {
        Map<String, Server> remoteServers = null;
        Provisioning prov = Provisioning.getInstance();
        for (Map.Entry<ItemId, Element> mptinfo : mountpoints.entrySet()) {
            try {
                // local mountpoints already have their targets serialized
                if (mptinfo.getValue() != null)
                    continue;

                Account owner = prov.get(Provisioning.AccountBy.id, mptinfo.getKey().getAccountId(), zsc.getAuthToken());
                if (owner == null)
                    continue;
                Server server = prov.getServer(owner);
                if (server == null)
                    continue;

                if (remoteServers == null)
                    remoteServers = new HashMap<String, Server>(3);
                remoteServers.put(owner.getId(), server);
            } catch (ServiceException e) { }
        }

        if (remoteServers != null && !remoteServers.isEmpty()) {
            Map<String, Element> remoteHierarchies = fetchRemoteHierarchies(octxt, zsc, remoteServers);
            for (Map.Entry<ItemId, Element> mptinfo : mountpoints.entrySet()) {
                // local mountpoints already have their targets serialized
                if (mptinfo.getValue() != null)
                    continue;
                ItemId iid = mptinfo.getKey();
                mptinfo.setValue(findRemoteFolder(iid.toString(mAuthenticatedAccountId), remoteHierarchies.get(iid.getAccountId())));
            }
        }
    }

    private Map<String, Element> fetchRemoteHierarchies(OperationContext octxt, ZimbraSoapContext zsc, Map<String, Server> remoteServers) {
        Map<String, Element> hierarchies = new HashMap<String, Element>();

        Element noop;
        try {
            noop = Element.create(zsc.getRequestProtocol(), MailConstants.GET_FOLDER_REQUEST).addAttribute(MailConstants.A_VISIBLE, true);
            if (!OperationContextData.getNeedGranteeName(octxt))
                noop.addAttribute(MailConstants.A_NEED_GRANTEE_NAME, false);
        } catch (ServiceException e) {
            // garbage in, nothing out...
            return hierarchies;
        }
        
        for (Map.Entry<String, Server> remote : remoteServers.entrySet()) {
            String accountId = remote.getKey();
            Server server = remote.getValue();
            
            try {
                ZimbraSoapContext zscProxy = new ZimbraSoapContext(zsc, accountId);
                zscProxy.setProxySession(getRemoteSessionId(server));

                ProxyTarget proxy = new ProxyTarget(server, zscProxy.getAuthToken(), URLUtil.getSoapURL(server, false));
                proxy.disableRetries().setTimeouts(10 * Constants.MILLIS_PER_SECOND);
                Pair<Element, Element> envelope = proxy.execute(noop.detach(), zscProxy);
                handleRemoteNotifications(server, envelope.getFirst(), true, true);
                hierarchies.put(accountId, envelope.getSecond().getOptionalElement(MailConstants.E_FOLDER));
            } catch (ServiceException e) { }
        }
        return hierarchies;
    }

    private static Element findRemoteFolder(String id, Element eFolder) {
        if (id == null || eFolder == null)
            return null;

        if (id.equalsIgnoreCase(eFolder.getAttribute(MailConstants.A_ID, null))) {
            // folder stubs (hierarchy placeholders for non-readable folders) contain only "id" and "name"
            boolean isStub = eFolder.getAttribute(MailConstants.A_SIZE, null) == null;
            return isStub ? null : eFolder.clone();
        }

        for (Element eSubfolder : eFolder.listElements()) {
            Element match = findRemoteFolder(id, eSubfolder);
            if (match != null)
                return match;
        }
        return null;
    }

    private static void transferMountpointContents(Element elem, OperationContext octxt, Map<ItemId, Element> mountpoints) throws ServiceException {
        if (elem == null)
            return;

        Element target = null;
        if (elem.getName().equals(MailConstants.E_MOUNT)) {
            ItemId iidTarget = new ItemId(elem.getAttribute(MailConstants.A_ZIMBRA_ID, null), (int) elem.getAttributeLong(MailConstants.A_REMOTE_ID, -1));
            target = mountpoints.get(iidTarget);
        }

        if (target == null) {
            // not a local mountpoint with a known target; recurse on children
            for (Element child : elem.listElements())
                transferMountpointContents(child, octxt, mountpoints);
        } else {
            transferMountpointContents(elem, target);
        }
    }

    public static void transferMountpointContents(Element elem, Element mptTarget) {
        // transfer folder counts to the serialized mountpoint from the serialized target folder
        transferLongAttribute(elem, mptTarget, MailConstants.A_UNREAD);
        transferLongAttribute(elem, mptTarget, MailConstants.A_NUM);
        transferLongAttribute(elem, mptTarget, MailConstants.A_SIZE);
        elem.addAttribute(MailConstants.A_OWNER_FOLDER_NAME, mptTarget.getAttribute(MailConstants.A_NAME, null));
        elem.addAttribute(MailConstants.A_URL, mptTarget.getAttribute(MailConstants.A_URL, null));
        elem.addAttribute(MailConstants.A_RIGHTS, mptTarget.getAttribute(MailConstants.A_RIGHTS, null));
        if (mptTarget.getAttribute(MailConstants.A_FLAGS, "").indexOf("u") != -1)
            elem.addAttribute(MailConstants.A_FLAGS, "u" + elem.getAttribute(MailConstants.A_FLAGS, "").replace("u", ""));

        // transfer ACL and child folders to the serialized mountpoint from the serialized remote folder
        for (Element child : mptTarget.listElements()) {
            if (child.getName().equals(MailConstants.E_ACL))
                elem.addUniqueElement(child.clone());
            else
                elem.addElement(child.clone());
        }
    }

    private static void transferLongAttribute(Element to, Element from, String attrName) {
        try {
            long remote = from.getAttributeLong(attrName, -1L);
            if (remote >= 0)
                to.addAttribute(attrName, remote);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("exception reading long attr from remote folder: " + attrName, e);
        } catch (Element.ContainerException e) {
            ZimbraLog.session.warn("exception adding remote folder attr to serialized mountpoint: " + attrName, e);
        }
    }


    public int getCurrentNotificationSequence() {
        synchronized (mSentChanges) {
            return mChanges.getSequence();
        }
    }

    public void acknowledgeNotifications(int sequence) {
        synchronized (mSentChanges) {
            if (mSentChanges == null || mSentChanges.isEmpty())
                return;
    
            if (sequence <= 0) {
                // if the client didn't send a valid "last sequence number", *don't* keep old changes around
                mSentChanges.clear();
            } else {
                // clear any notifications we now know the client has received
                for (Iterator<QueuedNotifications> it = mSentChanges.iterator(); it.hasNext(); ) {
                    if (it.next().getSequence() <= sequence)
                        it.remove();
                    else
                        break;
                }
            }
        }
    }

    /** Serializes cached notifications to a SOAP response header.
     *  <p>
     *  Adds a <tt>&lt;notify></tt> block to an existing <tt>&lt;context></tt>
     *  element, creating an enclosing <tt>&lt;context></tt> element if none
     *  is passed in.  This <tt>&lt;notify></tt> block contains information
     *  about all items deleted, created, or modified in the {@link Mailbox} since
     *  the last client interaction, without regard to the client's state/views.
     *  <p>
     *  For deleted items, only the item IDs are returned.  For created items, the
     *  entire item is serialized.  For modified items, only the modified attributes
     *  are included in the response.
     *  <p>
     *  Example:
     *  <pre>
     *     &lt;notify>
     *       &lt;deleted id="665,66,452,883"/>
     *       &lt;created>
     *         &lt;tag id="66" name="phlox" u="8"/>
     *         &lt;folder id="4353" name="a&p" u="2" l="1"/>
     *       &lt;/created>
     *       &lt;modified>
     *         &lt;tag id="65" u="0"/>
     *         &lt;m id="553" f="ua"/>
     *         &lt;note id="774" color="4">
     *           This is the new content.
     *         &lt;/note>
     *       &lt;/modified>
     *     &lt;/notify>
     *  </pre>
     *  Also adds a "last server change" changestamp to the <context> block.
     *  <p>
     * @param ctxt  An existing SOAP header &lt;context> element
     * @param zsc    The SOAP request context from the client's request
     * @param lastSequence  The highest notification-sequence-number that the client has
     *         received (0 means none)
     * @return The passed-in <tt>&lt;context></tt> element */
    public Element putNotifications(Element ctxt, ZimbraSoapContext zsc, int lastSequence) {
        Mailbox mbox = mMailbox;
        if (ctxt == null || mbox == null)
            return null;

        if (Provisioning.getInstance().allowsPingRemote()) {
            // if there are remote sessions that haven't been accessed in a while, ping them
            pingRemoteSessions(zsc);
        }

        // because ToXML functions can now call back into the Mailbox, don't hold any locks when calling putQueuedNotifications
        LinkedList<QueuedNotifications> notifications;
        synchronized (mSentChanges) {
            // send the "change" block:  <change token="555"/>
            ctxt.addUniqueElement(HeaderConstants.E_CHANGE).addAttribute(HeaderConstants.A_CHANGE_ID, mbox.getLastChangeID());

            // clear any notifications we now know the client has received
            acknowledgeNotifications(lastSequence);

            // cover ourselves in case a client is doing something really stupid...
            if (mSentChanges.size() > 20) {
                ZimbraLog.session.warn("clearing abnormally long notification change list due to misbehaving client");
                mSentChanges.clear();
            }

            if (mChanges.hasNotifications() || requiresRefresh(lastSequence)) {
                assert(mChanges.getSequence() >= 1);
                int newSequence = mChanges.getSequence() + 1;
                mSentChanges.add(mChanges);
                mChanges = new QueuedNotifications(newSequence); 
            }

            // mChanges must be empty at this point (everything moved into the mSentChanges list)
            assert(!mChanges.hasNotifications());

            // drop out if notify is off or if there is nothing to send
            if (mSentChanges.isEmpty())
                return ctxt;

            notifications = new LinkedList<QueuedNotifications>(mSentChanges);
        }

        // send all the old changes
        QueuedNotifications last = notifications.getLast();
        for (QueuedNotifications ntfn : notifications) {
            if (ntfn.hasNotifications() || ntfn == last)
                putQueuedNotifications(mbox, ntfn, ctxt, zsc);
        }

        return ctxt;
    }

    /** Size limit beyond which we suppress notifications on conversations
     *  belonging to other people's mailboxes.  We need to fetch the entire
     *  list of visible messages when serializing delegated conversations.
     *  If it looks like it'd be too expensive to fetch that list, we just
     *  skip the notification. */
    private static final int DELEGATED_CONVERSATION_SIZE_LIMIT = 50;

    private static final String A_ID = "id";

    /** Write a single instance of the PendingModifications structure into the 
     *  passed-in <ctxt> block. */
    protected void putQueuedNotifications(Mailbox mbox, QueuedNotifications ntfn, Element parent, ZimbraSoapContext zsc) {
        // create the base "notify" block:  <notify seq="6"/>
        Element eNotify = parent.addElement(ZimbraNamespace.E_NOTIFY);
        if (ntfn.getSequence() > 0)
            eNotify.addAttribute(HeaderConstants.A_SEQNO, ntfn.getSequence());

        OperationContext octxt = null;
        try {
            octxt = DocumentHandler.getOperationContext(zsc, this);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("error fetching operation context for: " + zsc.getAuthtokenAccountId(), e);
            return;
        }

        boolean debug = ZimbraLog.session.isDebugEnabled();

        PendingModifications pms = ntfn.mMailboxChanges;
        RemoteNotifications rns = ntfn.mRemoteChanges;

        // remote notifications get delivered *once*, then are discarded
        ntfn.mRemoteChanges = null;

        Element eDeleted = eNotify.addUniqueElement(ZimbraNamespace.E_DELETED);
        StringBuilder deletedIds = new StringBuilder();
        if (pms != null && pms.deleted != null && pms.deleted.size() > 0) {
            for (ModificationKey mkey : pms.deleted.keySet())
                addDeletedNotification(mkey, deletedIds);
        }
        if (rns != null && rns.deleted != null)
            deletedIds.append(deletedIds.length() == 0 ? "" : ",").append(rns.deleted);

        boolean hasLocalCreates = pms != null && pms.created != null && !pms.created.isEmpty();
        boolean hasRemoteCreates = rns != null && rns.created != null && !rns.created.isEmpty();
        if (hasLocalCreates || hasRemoteCreates) {
            Element eCreated = eNotify.addUniqueElement(ZimbraNamespace.E_CREATED);
            if (hasLocalCreates) {
                for (MailItem item : pms.created.values()) {
                    ItemIdFormatter ifmt = new ItemIdFormatter(mAuthenticatedAccountId, item.getMailbox(), false);
                    try {
                        Element elem = ToXML.encodeItem(eCreated, ifmt, octxt, item, ToXML.NOTIFY_FIELDS);
                        // special-case notifications for new mountpoints in the authenticated user's mailbox
                        if (item instanceof Mountpoint && mbox == item.getMailbox()) {
                            Map<ItemId, Element> mountpoints = new HashMap<ItemId, Element>(2);
                            expandLocalMountpoint(octxt, (Mountpoint) item, eCreated.getFactory(), mountpoints);
                            expandRemoteMountpoints(octxt, zsc, eCreated.getFactory(), mountpoints);
                            transferMountpointContents(elem, octxt, mountpoints);
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.session.warn("error encoding item " + item.getId(), e);
                        return;
                    }
                }
                // sanity-check the returned element
                if (!eCreated.hasChildren() && debug)
                    ZimbraLog.session.debug("no serialied creates for item set: " + pms.created.keySet());
            }
            if (hasRemoteCreates) {
                if (debug)
                    ZimbraLog.session.debug("adding " + rns.created.size() + " proxied creates");
                for (Element elt : rns.created)
                    eCreated.addElement(elt.detach());
            }
        }

        boolean hasLocalModifies = pms != null && pms.modified != null && !pms.modified.isEmpty();
        boolean hasRemoteModifies = rns != null && rns.modified != null && !rns.modified.isEmpty();
        if (hasLocalModifies || hasRemoteModifies) {
            Element eModified = eNotify.addUniqueElement(ZimbraNamespace.E_MODIFIED);
            if (hasLocalModifies) {
                for (Change chg : pms.modified.values()) {
                    if (chg.why != 0 && chg.what instanceof MailItem) {
                        MailItem item = (MailItem) chg.what;
                        // don't serialize out changes on too-large delegated conversation
                        if (mbox != item.getMailbox() && item instanceof Conversation && ((Conversation) item).getMessageCount() > DELEGATED_CONVERSATION_SIZE_LIMIT) {
                            if (debug)
                                ZimbraLog.session.debug("skipping serialization of too-large remote conversation: " + new ItemId(item));
                            continue;
                        }
    
                        ItemIdFormatter ifmt = new ItemIdFormatter(mAuthenticatedAccountId, item.getMailbox(), false);
                        try {
                            Element elt = ToXML.encodeItem(eModified, ifmt, octxt, item, chg.why);
                            if (elt == null) {
                                ModificationKey mkey = new ModificationKey(item);
                                addDeletedNotification(mkey, deletedIds);
                                if (debug)
                                    ZimbraLog.session.debug("marking nonserialized item as a delete: " + mkey);
                            }
                        } catch (ServiceException e) {
                            ZimbraLog.session.warn("error encoding item " + item.getId(), e);
                            return;
                        }
                    } else if (chg.why != 0 && chg.what instanceof Mailbox) {
                        ToXML.encodeMailbox(eModified, octxt, (Mailbox) chg.what, chg.why);
                    }
                }
                // sanity-check the returned element
                if (!eModified.hasChildren() && debug)
                    ZimbraLog.session.debug("no serialied modifies for item set: " + pms.modified.keySet());
            }
            if (hasRemoteModifies) {
                if (debug)
                    ZimbraLog.session.debug("adding " + rns.modified.size() + " proxied modifies");
                for (Element elt : rns.modified)
                    eModified.addElement(elt.detach());
            }
        }
        
        if (ntfn.mIMNotifications != null && ntfn.mIMNotifications.size() > 0) {
            Element eIM = eNotify.addUniqueElement(ZimbraNamespace.E_IM);
            for (IMNotification imn : ntfn.mIMNotifications) {
                try {
                    imn.toXml(eIM);
                } catch (ServiceException e) {
                    ZimbraLog.session.warn("error serializing IM notification; skipping", e);
                }
            }
        }

        if (deletedIds == null || deletedIds.length() == 0)
            eDeleted.detach();
        else
            eDeleted.addAttribute(A_ID, deletedIds.toString());
    }

    private void addDeletedNotification(ModificationKey mkey, StringBuilder deletedIds) {
        if (deletedIds.length() != 0)
            deletedIds.append(',');
        // should be using the ItemIdFormatter, but I'm preoptimizing here
        if (!mkey.getAccountId().equals(mAuthenticatedAccountId))
            deletedIds.append(mkey.getAccountId()).append(':');
        deletedIds.append(mkey.getItemId());
    }


    public void clearCachedQueryResults() throws ServiceException {
        synchronized (this) {
            try {
                if (mQueryResults != null)
                    mQueryResults.doneWithSearchResults();
            } finally {
                mQueryStr = "";
                mGroupBy  = "";
                mSortBy   = "";
                mQueryResults = null;
            }
        }
    }
    
    public void putQueryResults(String queryStr, String groupBy, String sortBy, ZimbraQueryResults res) throws ServiceException {
        synchronized (this) {
            clearCachedQueryResults();
            mQueryStr = queryStr;
            mGroupBy = groupBy;
            mSortBy = sortBy;
            mQueryResults = res;
        }
    }
    
    public ZimbraQueryResults getQueryResults(String queryStr, String groupBy, String sortBy) {
        synchronized (this) {
            if (mQueryStr.equals(queryStr) && mGroupBy.equals(groupBy) && mSortBy.equals(sortBy))
                return mQueryResults;
            else
                return null;
        }
    }

    @Override public void cleanup() {
        try {
            clearCachedQueryResults();
        } catch (ServiceException e) {
        	ZimbraLog.session.warn("ServiceException while cleaning up Session", e);
        }
    }
}
