/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 9, 2004
 */
package com.zimbra.cs.session;

import java.util.ArrayList;
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
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.mail.GetFolder;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author tim
 * 
 * Add your own get/set methods here for session data.
 */
public class SoapSession extends Session {
    private class DelegateSession extends Session {
        private long mNextFolderCheck;
        private Set<Integer> mVisibleFolderIds;

        DelegateSession(String authId, String targetId) {
            super(authId, targetId, Type.SOAP);
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

        @Override public void cleanup()  { }

        private synchronized boolean calculateVisibleFolders(boolean force) throws ServiceException {
            long now = System.currentTimeMillis();
            if (!force && mNextFolderCheck > now)
                return mVisibleFolderIds != null;

            mNextFolderCheck = now + 5 * Constants.MILLIS_PER_MINUTE;

            Mailbox mbox = mMailbox;
            Set<Folder> visible = mbox == null ? null : mbox.getVisibleFolders(new OperationContext(getAuthenticatedAccountId()));
            Set<Integer> ids = null;
            if (visible != null) {
                ids = new HashSet<Integer>(visible.size());
                for (Folder folder : visible)
                    ids.add(folder.getId());
            }
            return (mVisibleFolderIds = ids) != null;
        }

        private PendingModifications filterNotifications(PendingModifications pms) throws ServiceException {
            // first, recalc visible folders if any folders got created or moved or had their ACL changed
            boolean recalc = false;
            if (pms.created != null && !pms.created.isEmpty()) {
                for (MailItem item : pms.created.values()) {
                    if (item instanceof Folder) {
                        recalc = true;  break;
                    }
                }
            }
            if (!recalc && pms.modified != null && !pms.modified.isEmpty()) {
                for (Change chg : pms.modified.values()) {
                    if ((chg.why & (Change.MODIFIED_ACL | Change.MODIFIED_FOLDER)) != 0 && chg.what instanceof Folder) {
                        recalc = true;  break;
                    }
                }
            }
            if (recalc) {
                if (!calculateVisibleFolders(true))
                    return pms;
            }
            assert(mVisibleFolderIds != null);


            PendingModifications filtered = new PendingModifications();
            filtered.changedTypes = pms.changedTypes;
            if (pms.deleted != null && !pms.deleted.isEmpty()) {
                filtered.recordDeleted(pms.deleted.keySet(), pms.changedTypes);
            }
            if (pms.created != null && !pms.created.isEmpty()) {
                for (MailItem item : pms.created.values()) {
                    if (mVisibleFolderIds.contains(item instanceof Folder ? item.getId() : item.getFolderId()))
                        filtered.recordCreated(item);
                }
            }
            if (pms.modified != null && !pms.modified.isEmpty()) {
                for (Change chg : pms.modified.values()) {
                    if (!(chg.what instanceof MailItem))
                        continue;
                    MailItem item = (MailItem) chg.what;
                    boolean visible = mVisibleFolderIds.contains(item.getFolderId());
                    if ((chg.why & Change.MODIFIED_FOLDER) != 0) {
                        // a move between visible folders ends up as a delete and a create, but that should be OK
                        filtered.recordDeleted(item);
                        if (visible)
                            filtered.recordCreated(item);
                    } else if (visible) {
                        filtered.recordModified(item, chg.why);
                    }
                }
            }
            return filtered;
        }
    }

    private class RemoteSessionInfo {
        final String mServerId, mSessionId;
        long mLastPoll;

        RemoteSessionInfo(String sessionId, String serverId, long lastPoll) {
            mSessionId = sessionId;  mServerId = serverId;  mLastPoll = lastPoll;
        }
    }

    private static class QueuedNotifications {
        /** IMNotifications are strictly sequential right now */
        public List<IMNotification> mIMNotifications;
        public PendingModifications mMailboxChanges;

        /** used by the Session object to ensure that notifications are reliably
         * received by the listener */
        private int mSequence;
        int getSequence()  { return mSequence; }

        QueuedNotifications(int seqno)  { mSequence = seqno; }

        boolean hasNotifications() {
            return getNotificationCount() > 0 || (mIMNotifications != null && mIMNotifications.size() > 0);
        }

        int getNotificationCount() {
            return mMailboxChanges == null ? 0 : mMailboxChanges.getNotificationCount();
        }

        void addNotification(IMNotification imn) {
            if (mIMNotifications == null) 
                mIMNotifications = new LinkedList<IMNotification>();
            mIMNotifications.add(imn);
        }

        void addNotification(PendingModifications pms) {
            if (mMailboxChanges == null) 
                mMailboxChanges = new PendingModifications();
            mMailboxChanges.add(pms);
        }

        void clearMailboxChanges() {
            mMailboxChanges = null;
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

    // Read/write access to all these members requires synchronizing on "mSentChanges".
    private int mForceRefresh;
    private LinkedList<QueuedNotifications> mSentChanges = new LinkedList<QueuedNotifications>();
    private QueuedNotifications mChanges = new QueuedNotifications(1);

    private PushChannel mPushChannel = null;
    private Map<String, DelegateSession> mDelegateSessions;
    private Map<String, RemoteSessionInfo> mRemoteSessions;

    private static final long SOAP_SESSION_TIMEOUT_MSEC = 10 * Constants.MILLIS_PER_MINUTE;

    private static final int MAX_QUEUED_NOTIFICATIONS = LC.zimbra_session_max_pending_notifications.intValue();


    /** Creates a <tt>SoapSession</tt> owned by the given account and
     *  listening on its {@link Mailbox}.
     * @see Session#register() */
    public SoapSession(String authenticatedId) {
        super(authenticatedId, Session.Type.SOAP);
    }

    @Override public SoapSession register() throws ServiceException {
        super.register();
        mRecentMessages = mMailbox.getRecentMessageCount();
        mPreviousAccess = mMailbox.getLastSoapAccessTime();
        return this;
    }

    @Override public SoapSession unregister() {
        // when the session goes away, record the timestamp of the last write op to the database
        if (mLastWrite != -1 && mMailbox != null) {
            try {
                mMailbox.recordLastSoapAccessTime(mLastWrite);
            } catch (Throwable t) {
                ZimbraLog.session.warn("exception recording unloaded session's last access time", t);
            }
        }
        synchronized (this) {
            // unloading a SoapSession also must unload all its delegates
            if (mDelegateSessions != null) {
                List<DelegateSession> delegates = new ArrayList<DelegateSession>(mDelegateSessions.values());
                for (DelegateSession ds : delegates)
                    ds.unregister();
                mDelegateSessions = null;
            }
        }
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


    public DelegateSession getDelegateSession(String targetAccountId) {
        targetAccountId = targetAccountId.toLowerCase();
        synchronized (this) {
            if (mDelegateSessions == null)
                mDelegateSessions = new HashMap<String, DelegateSession>();
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
        if (mDelegateSessions == null)
            return;
        synchronized (this) {
            boolean removed = mDelegateSessions.remove(ds.mTargetAccountId.toLowerCase()) != null;
            if (!removed)
                return;
        }
        synchronized (mSentChanges) {
            mForceRefresh = mChanges.getSequence();
        }
    }


    @Override protected long getSessionIdleLifetime() {
        return SOAP_SESSION_TIMEOUT_MSEC;
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
        public int getLastKnownSeqNo();
        public ZimbraSoapContext getSoapContext();
        public void notificationsReady() throws ServiceException; 
    }

    public static enum RegisterNotificationResult {
        NO_NOTIFY,      // notifications not available for this session
        DATA_READY,     // notifications already here
        BLOCKING;       // none here yet, wait
    }

    /** Record that a push channel has come online.
     *
     * @return TRUE if the PushChannel is waiting (no data and notifications
     *         turned on), or FALSE if the channel is not waiting.
     * @param sc  The push channel. */
    public RegisterNotificationResult registerNotificationConnection(PushChannel sc) throws ServiceException {
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
                boolean notifying = mChanges.hasNotifications();
                int lastSeqNo = sc.getLastKnownSeqNo();
                boolean isEmpty = mSentChanges.isEmpty();
                dataReady = notifying || (mChanges.getSequence() > lastSeqNo + 1 && !isEmpty);
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
            notifyPushChannel(true);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in notifyIM", e);
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
     * @param changeId  The sync-token change id of the change.
     * @param pms       A set of new change notifications from our Mailbox. */
    @Override public void notifyPendingChanges(PendingModifications pms, int changeId, Session source) {
        if (pms == null || !pms.hasNotifications() || mMailbox == null)
            return;

        if (source == this) {
            // on the session's first write op, record the timestamp to the database
            boolean firstWrite = mLastWrite == -1;
            mLastWrite = System.currentTimeMillis();
            if (firstWrite) {
                try {
                    mMailbox.recordLastSoapAccessTime(mLastWrite);
                } catch (ServiceException e) {
                    ZimbraLog.session.warn("ServiceException in notifyPendingChanges ", e);
                }
            }
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
                        if (isReceived)
                            mRecentMessages++;
                    }
                }
            }
        }

        handleNotifications(pms, source == this);
    }

    void handleNotifications(PendingModifications pms, boolean fromThisSession) {
        try {
            // update the set of notifications not yet sent to the client
            cacheNotifications(pms, fromThisSession);
            // if we're in a hanging no-op, alert the client that there are changes
        	notifyPushChannel(true);
            // FIXME: this query result cache purge seems a little aggressive
        	clearCachedQueryResults();
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in notifyPendingChanges ", e);
        }
    }

    private void cacheNotifications(PendingModifications pms, boolean fromThisSession) {
        synchronized (mSentChanges) {
            // if we're going to be sending a <refresh> anyway, there's no need to record these changes
            int currentSequence = getCurrentNotificationSequence();
            if (mForceRefresh == currentSequence && !fromThisSession)
                return;

            // XXX: should constrain to folders, tags, and stuff relevant to the current query?

            // determine whether this set of notifications would cause the cached set to overflow
            if (mForceRefresh != currentSequence && MAX_QUEUED_NOTIFICATIONS > 0) {
                // XXX: more accurate would be to combine pms and mChanges and take the count...
                int count = pms.getNotificationCount() + mChanges.getNotificationCount();
                if (count > MAX_QUEUED_NOTIFICATIONS) {
                    // if we've overflowed, jettison the pending change set
                    mChanges.clearMailboxChanges();
                    mForceRefresh = currentSequence;
                }

                for (QueuedNotifications ntfn : mSentChanges) {
                    count += ntfn.getNotificationCount();
                    if (count > MAX_QUEUED_NOTIFICATIONS) {
                        ntfn.clearMailboxChanges();
                        mForceRefresh = Math.max(mForceRefresh, ntfn.getSequence());
                    }
                }
            }

            if (mForceRefresh == currentSequence && !fromThisSession)
                return;
            // if we're here, these changes either
            //   a) do not cause the session's notification cache to overflow, or
            //   b) originate from this session and hence must be notified back to the session
            mChanges.addNotification(pms);
        }
    }

    public void forcePush() {
        try {
            notifyPushChannel(false);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in forcePush", e);
        }
    }

    private void notifyPushChannel(boolean clearChannel) throws ServiceException {
        // don't have to lock the Mailbox before locking the Session to avoid deadlock because we're not calling any ToXML functions
        synchronized (this) {
            if (mPushChannel != null) {
                mPushChannel.notificationsReady();
                if (clearChannel)
                	mPushChannel = null;
            }
        }
    }


    public boolean requiresRefresh(int lastSequence) {
        synchronized (mSentChanges) {
            if (lastSequence <= 0)
                return mForceRefresh == getCurrentNotificationSequence();
            else
                return mForceRefresh > Math.min(lastSequence, getCurrentNotificationSequence());
        }
    }

    private static final String A_ID = "id";

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
        if (mMailbox == null)
            return;

        synchronized (mSentChanges) {
            for (QueuedNotifications ntfn : mSentChanges)
                ntfn.clearMailboxChanges();
        }

        Element eRefresh = ctxt.addUniqueElement(ZimbraNamespace.E_REFRESH);
        eRefresh.addAttribute(AccountConstants.E_VERSION, BuildInfo.FULL_VERSION, Element.Disposition.CONTENT);

        OperationContext octxt = DocumentHandler.getOperationContext(zsc, this);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        // Lock the mailbox but not the "this" object, to avoid deadlock
        // with another thread that calls a Session method from within a
        // synchronized Mailbox method.
        synchronized (mMailbox) {
            // dump current mailbox status (currently just size)
            ToXML.encodeMailbox(eRefresh, mMailbox);

            // dump all tags under a single <tags> parent
            List<Tag> tags = mMailbox.getTagList(octxt);
            if (tags != null && tags.size() > 0) {
                Element eTags = eRefresh.addUniqueElement(ZimbraNamespace.E_TAGS);
                for (Tag tag : tags) {
                    if (tag != null && !(tag instanceof Flag))
                        ToXML.encodeTag(eTags, ifmt, tag);
                }
            }

            // dump recursive folder hierarchy starting at USER_ROOT (i.e. folders visible to the user)
            GetFolder.FolderNode root = GetFolder.getFolderTree(octxt, mMailbox, null, false);
            expandMountpoints(octxt, root);
            GetFolder.encodeFolderNode(ifmt, octxt, eRefresh, root);
        }
    }

    private void expandMountpoints(OperationContext octxt, GetFolder.FolderNode node) {
        if (node.mFolder == null) {
            return;
        } else if (node.mFolder instanceof Mountpoint) {
            Mountpoint mpt = (Mountpoint) node.mFolder;
            try {
                Account owner = Provisioning.getInstance().get(Provisioning.AccountBy.id, mpt.getOwnerId());
                // FIXME: not handling mountpoints pointing to a different server
                if (owner == null || !Provisioning.onLocalServer(owner))
                    return;

                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);
                GetFolder.FolderNode remote = GetFolder.getFolderTree(octxt, mbox, new ItemId(mbox, mpt.getRemoteId()), false);
                if (remote != null) {
                    node.mSubfolders.addAll(remote.mSubfolders);
                    // fault in a delegate session because there's actually something to listen on...
                    getDelegateSession(mpt.getOwnerId());
                }
            } catch (ServiceException e) {
                return;
            }
        } else {
            for (GetFolder.FolderNode child : node.mSubfolders)
                expandMountpoints(octxt, child);
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
     *  Adds a <code>&lt;notify></code> block to an existing <code>&lt;context></code>
     *  element, creating an enclosing <code>&lt;context></code> element if none
     *  is passed in.  This <code>&lt;notify></code> block contains information
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
     * @return The passed-in <code>&lt;context></code> element */
    public Element putNotifications(Element ctxt, ZimbraSoapContext zsc, int lastSequence) {
        if (ctxt == null || mMailbox == null)
            return null;

        String explicitAcct = getAuthenticatedAccountId().equals(zsc.getAuthtokenAccountId()) ? null : getAuthenticatedAccountId();

        // because ToXML functions can now call back into the Mailbox, don't hold any locks when calling putQueuedNotifications
        LinkedList<QueuedNotifications> notifications;
        synchronized (mSentChanges) {
            // send the <change> block
            // <change token="555" [acct="4f778920-1a84-11da-b804-6b188d2a20c4"]/>
            ctxt.addUniqueElement(HeaderConstants.E_CHANGE)
                .addAttribute(HeaderConstants.A_CHANGE_ID, mMailbox.getLastChangeID())
                .addAttribute(HeaderConstants.A_ACCOUNT_ID, explicitAcct);

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
                putQueuedNotifications(ntfn, ctxt, zsc, explicitAcct);
        }

        return ctxt;
    }

    /**
     * Write a single instance of the PendingModifications structure into the 
     * passed-in <notify> block 
     * @param ntfn
     * @param parent
     * @param zsc
     * @param explicitAcct
     */
    private void putQueuedNotifications(QueuedNotifications ntfn, Element parent, ZimbraSoapContext zsc, String explicitAcct) {
        assert(ntfn.getSequence() > 0);

        // <notify [acct="4f778920-1a84-11da-b804-6b188d2a20c4"]/>
        Element eNotify = parent.addElement(ZimbraNamespace.E_NOTIFY)
                                .addAttribute(HeaderConstants.A_ACCOUNT_ID, explicitAcct)
                                .addAttribute(HeaderConstants.A_SEQNO, ntfn.getSequence());


        OperationContext octxt = null;
        try {
            octxt = DocumentHandler.getOperationContext(zsc, this);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("error fetching operation context for: " + zsc.getAuthtokenAccountId(), e);
            return;
        }

        PendingModifications pms = ntfn.mMailboxChanges;

        if (pms != null && pms.deleted != null && pms.deleted.size() > 0) { 
            StringBuilder ids = new StringBuilder();
            for (PendingModifications.ModificationKey mkey : pms.deleted.keySet()) {
                if (ids.length() != 0)
                    ids.append(',');
                // should be using the ItemIdFormatter, but I'm preoptimizing here
                if (!mkey.getAccountId().equals(mAuthenticatedAccountId))
                    ids.append(mkey.getAccountId()).append(':');
                ids.append(mkey.getItemId());
            }
            Element eDeleted = eNotify.addUniqueElement(ZimbraNamespace.E_DELETED);
            eDeleted.addAttribute(A_ID, ids.toString());
        }

        if (pms != null && pms.created != null && pms.created.size() > 0) {
            Element eCreated = eNotify.addUniqueElement(ZimbraNamespace.E_CREATED);
            for (MailItem item : pms.created.values()) {
                ItemIdFormatter ifmt = new ItemIdFormatter(mAuthenticatedAccountId, item.getMailbox(), false);
                try {
                    ToXML.encodeItem(eCreated, ifmt, octxt, item, ToXML.NOTIFY_FIELDS);
                } catch (ServiceException e) {
                    ZimbraLog.session.warn("error encoding item " + item.getId(), e);
                    return;
                }
            }
        }

        if (pms != null && pms.modified != null && pms.modified.size() > 0) {
            Element eModified = eNotify.addUniqueElement(ZimbraNamespace.E_MODIFIED);
            for (Change chg : pms.modified.values()) {
                if (chg.why != 0 && chg.what instanceof MailItem) {
                    MailItem item = (MailItem) chg.what;
                    ItemIdFormatter ifmt = new ItemIdFormatter(mAuthenticatedAccountId, item.getMailbox(), false);
                    try {
                        ToXML.encodeItem(eModified, ifmt, octxt, item, chg.why);
                    } catch (ServiceException e) {
                        ZimbraLog.session.warn("error encoding item " + item.getId(), e);
                        return;
                    }
                } else if (chg.why != 0 && chg.what instanceof Mailbox) {
                    ToXML.encodeMailbox(eModified, (Mailbox) chg.what, chg.why);
                }
            }
        }
        
        if (ntfn.mIMNotifications != null && ntfn.mIMNotifications.size() > 0) {
            Element eIM = eNotify.addUniqueElement(ZimbraNamespace.E_IM);
            for (IMNotification imn : ntfn.mIMNotifications) {
                try {
                    imn.toXml(eIM);
                } catch (ServiceException e) {
                    e.printStackTrace();
                }
            }
        }
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
