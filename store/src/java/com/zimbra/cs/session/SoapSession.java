/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.ContainerException;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Comment;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Message.EventFlag;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.OperationContextData;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.mail.GetFolder;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.IOUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ProxyTarget;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;

/**
 * @since Nov 9, 2004
 */
public class SoapSession extends Session {
    public class DelegateSession extends Session {
        private long mNextFolderCheck;
        private Set<Integer> mVisibleFolderIds;
        private boolean mParentHasFullAccess = false;

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

        @SuppressWarnings("rawtypes")
        @Override public void notifyPendingChanges(PendingModifications pmsIn, int changeId, Session source) {
            PendingLocalModifications pms = (PendingLocalModifications) pmsIn;
            try {
                if (calculateVisibleFolders(false))
                    pms = filterNotifications(pms);
                if (pms != null && pms.hasNotifications())
                    handleNotifications(pms, source == this || source == SoapSession.this);
            } catch (ServiceException e) {
                ZimbraLog.session.warn("exception during delegated notifyPendingChanges", e);
            }
        }

        @Override public void notifyExternalEvent(ExternalEventNotification extra) {
            Account authAccount = null;
            try {
                authAccount = Provisioning.getInstance().getAccountById(getAuthenticatedAccountId());
            } catch (ServiceException e) {
            }
            if (extra.canAccess(authAccount)) {
                SoapSession.this.notifyExternalEvent(extra);
            }
        }

        /**
         * Returns true if the MailItem should be excluded from notification serialization
         * @throws ServiceException
         */
        protected boolean skipChangeSerialization(Mailbox mbox, MailItem item) throws ServiceException {
            // don't serialize out changes on too-large delegated conversation
            return (mbox != item.getMailbox() && item instanceof Conversation && ((Conversation) item).getMessageCount() > DELEGATED_CONVERSATION_SIZE_LIMIT && !mParentHasFullAccess);
        }

        /**
         * Fetch account level access and store it in cache to be used in skipChangeSerialization
         * We assume here that ACL will never change between this call and the call to skipChangeSerialization()
         * We also assume that it *might* change between invocations of calculateVisibileFolders (i.e. check accessmgr each time)
         * @throws ServiceException
         */
        protected void cacheAccountAccess(String authedAcctId, String targetAcctId) throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            mParentHasFullAccess = AccessManager.getInstance().canAccessAccount(
                prov.get(AccountBy.id, authedAcctId), prov.get(AccountBy.id, targetAcctId),
                getParentSession().asAdmin());
        }

        private boolean calculateVisibleFolders(boolean force) throws ServiceException {
            long now = System.currentTimeMillis();

            Mailbox mbox = this.getMailboxOrNull();
            cacheAccountAccess(mAuthenticatedAccountId, mTargetAccountId);
            if (mbox == null) {
                mVisibleFolderIds = Collections.emptySet();
                return true;
            }

            try (final MailboxLock l = mbox.lock(true)) {
                l.lock();
                if (!force && (mNextFolderCheck < 0 || mNextFolderCheck > now)) {
                    return mVisibleFolderIds != null;
                }

                Set<Integer> visible = MailItem.toId(mbox.getVisibleFolders(new OperationContext(getAuthenticatedAccountId())));
                mVisibleFolderIds = visible;
                mNextFolderCheck = DebugConfig.visibileFolderRecalcInterval > 0 ? now + DebugConfig.visibileFolderRecalcInterval : -1;
                return visible != null;
            }
        }

        private boolean folderRecalcRequired(PendingLocalModifications pms) {
            boolean recalc = false;
            if (pms.created != null && !pms.created.isEmpty()) {
                for (BaseItemInfo item : pms.created.values()) {
                    if (item instanceof Folder)
                        return true;
                }
            }
            if (!recalc && pms.modified != null && !pms.modified.isEmpty()) {
                for (Change chg : pms.modified.values()) {
                    if ((chg.why & (Change.ACL | Change.FOLDER)) != 0 && chg.what instanceof Folder) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static final int BASIC_CONVERSATION_FLAGS = Change.FLAGS | Change.TAGS | Change.UNREAD;
        private static final int MODIFIED_CONVERSATION_FLAGS = BASIC_CONVERSATION_FLAGS | Change.SIZE  | Change.SENDERS;

        private PendingLocalModifications filterNotifications(PendingLocalModifications pms) throws ServiceException {
            // first, recalc visible folders if any folders got created or moved or had their ACL changed
            if (folderRecalcRequired(pms) && !calculateVisibleFolders(true)) {
                return pms;
            }
            Set<Integer> visible = mVisibleFolderIds;
            if (visible == null) {
                return pms;
            }

            OperationContext octxt = new OperationContext(getAuthenticatedAccountId());
            PendingLocalModifications filtered = new PendingLocalModifications();
            filtered.changedTypes = pms.changedTypes;
            filtered.addChangedParentFolderIds(pms.getChangedParentFolders());  // Not 100% sure this is best but it is conservative
            if (pms.deleted != null && !pms.deleted.isEmpty()) {
                filtered.recordDeleted(pms.deleted);
            }
            if (pms.created != null && !pms.created.isEmpty()) {
                for (BaseItemInfo item : pms.created.values()) {
                    if (item instanceof Conversation ||
                            visible.contains(item instanceof Folder ? item.getIdInMailbox() : item.getFolderIdInMailbox())) {
                        filtered.recordCreated(item);
                    } else if (item instanceof Comment) {
                        try {
                            this.getMailboxOrNull().getItemById(octxt, ((Comment) item).getParentId(), MailItem.Type.UNKNOWN);
                            filtered.recordCreated(item);
                        } catch (ServiceException e) {
                            // no permission.  ignore the item.
                        }
                    }
                }
            }
            if (pms.modified != null && !pms.modified.isEmpty()) {
                for (Change chg : pms.modified.values()) {
                    if (chg.what instanceof MailItem) {
                        MailItem item = (MailItem) chg.what;
                        if (skipChangeSerialization(getParentSession().getMailboxOrNull(), item)) {
                            if (ZimbraLog.session.isDebugEnabled()) {
                                ZimbraLog.session.debug("skipping serialization of too-large remote conversation: " +
                                                                new ItemId(item));
                            }
                            continue;
                        }
                        boolean isVisible =
                                visible.contains(item instanceof Folder ? item.getId() : item.getFolderId());
                        boolean moved = (chg.why & Change.FOLDER) != 0;
                        if (item instanceof Conversation) {
                            filtered.recordModified(item, chg.why | MODIFIED_CONVERSATION_FLAGS,
                                    (MailItem) chg.preModifyObj);
                        } else if (isVisible) {
                            filtered.recordModified(item, chg.why, (MailItem) chg.preModifyObj);
                            // if it's an unmoved visible message and it had a tag/flag/unread change,
                            // make sure the conv shows up in the modified or created list
                            if (item instanceof Message && (moved || (chg.why & BASIC_CONVERSATION_FLAGS) != 0)) {
                                forceConversationModification(
                                        (Message) item, chg, pms, filtered,
                                        moved ? MODIFIED_CONVERSATION_FLAGS : BASIC_CONVERSATION_FLAGS);
                            }
                        } else if (moved) {
                            filtered.recordDeleted((MailItem) chg.preModifyObj);
                            // if it's a message and it's moved, make sure the conv shows up in the
                            // modified or created list
                            if (item instanceof Message) {
                                forceConversationModification(
                                        (Message) item, chg, pms, filtered, MODIFIED_CONVERSATION_FLAGS);
                            }
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

        private void forceConversationModification(
                Message msg, Change chg, PendingLocalModifications pms, PendingLocalModifications filtered, int changeMask) throws ServiceException {
            int convId = msg.getConversationId();
            Mailbox mbox = msg.getMailbox();
            ModificationKey mkey = new ModificationKey(mbox.getAccountId(), convId);
            Change existing;
            if (pms.created != null && pms.created.containsKey(mkey)) {
                // do nothing
            } else if (pms.modified != null && (existing = pms.modified.get(mkey)) != null) {
                filtered.recordModified((MailItem) existing.what, existing.why | changeMask,
                        (MailItem) existing.preModifyObj);
            } else {
                try {
                    filtered.recordModified(mbox.getConversationById(null, convId), changeMask);
                } catch (ServiceException e) {
                    ZimbraLog.session.warn("exception during forceConversationModification", e);
                }
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

    public static class RemoteNotifications {
        int count = -1;
        String deleted;
        List<Element> created;
        List<Element> modified;
        List<Element> activities;

        public RemoteNotifications(Element eNotify) {
            if (eNotify == null)
                return;
            Element eSection;
            if ((eSection = eNotify.getOptionalElement(ZimbraNamespace.E_DELETED)) != null)
                deleted = eSection.getAttribute(A_ID, null);
            if ((eSection = eNotify.getOptionalElement(ZimbraNamespace.E_CREATED)) != null)
                created = new ArrayList<Element>(eSection.listElements());
            if ((eSection = eNotify.getOptionalElement(ZimbraNamespace.E_MODIFIED)) != null)
                modified = new ArrayList<Element>(eSection.listElements());
            activities = eNotify.listElements(MailConstants.E_A);
            if (activities.isEmpty()) activities = null;
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

            if (activities == null)           activities = rns.activities;
            else if (rns.activities != null)  activities.addAll(rns.activities);

            count = (count >= 0 && rns.count >= 0 ? count + rns.count : -1);
            return this;
        }

        int getScaledNotificationCount() {
            if (count == -1) {
                count = 0;
                if (deleted != null)   count += StringUtil.countOccurrences(deleted, ',') / 4 + 1;
                if (created != null)   count += created.size();
                if (modified != null)  count += modified.size();
                if (activities != null)  count += activities.size();
            }
            return count;
        }

        boolean hasNotifications() {
            if (deleted != null && !deleted.equals(""))   return true;
            if (created != null && !created.isEmpty())    return true;
            if (modified != null && !modified.isEmpty())  return true;
            if (activities != null && !activities.isEmpty())  return true;
            return false;
        }
    }

    class QueuedNotifications {
        /** ExternalEventNotifications are kept sequentially */
        List<ExternalEventNotification> mExternalNotifications;
        PendingLocalModifications mMailboxChanges;
        RemoteNotifications mRemoteChanges;
        boolean mHasLocalChanges;

        /** used by the Session object to ensure that notifications are reliably
         *  received by the listener */
        private final int mSequence;
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
            if (mExternalNotifications != null && !mExternalNotifications.isEmpty())
                return true;
            return false;
        }

        int getScaledNotificationCount() {
            return (mMailboxChanges == null ? 0 : mMailboxChanges.getScaledNotificationCount()) +
                   (mRemoteChanges == null  ? 0 : mRemoteChanges.getScaledNotificationCount());
        }

        void addNotification(ExternalEventNotification extra) {
            if (mExternalNotifications == null)
                mExternalNotifications = new LinkedList<ExternalEventNotification>();
            mExternalNotifications.add(extra);
        }

        void addNotification(PendingLocalModifications pms) {
            if (pms == null || !pms.hasNotifications())
                return;
            if (mMailboxChanges == null)
                mMailboxChanges = new PendingLocalModifications();
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

    static final long SOAP_SESSION_TIMEOUT_MSEC = Math.max(5, LC.zimbra_session_timeout_soap.intValue()) * Constants.MILLIS_PER_SECOND;
    // if a keepalive request to a remote session failed, how long to wait before a new ping is permitted
    private static final long MINIMUM_PING_RETRY_TIME = 30 * Constants.MILLIS_PER_SECOND;
    private static final int MAX_QUEUED_NOTIFICATIONS = LC.zimbra_session_max_pending_notifications.intValue();

    // Read/write access to all these members requires synchronizing on "this".
    private String queryString = "";
    private String groupBy = "";
    private String sortBy = "";
    private ZimbraQueryResults queryResults;
    private int  recentMessages;
    private long previousAccess = -1;
    private long lastWrite      = -1;

    // read/write access to all these members requires synchronizing on "mSentChanges"
    protected int forceRefresh;
    protected LinkedList<QueuedNotifications> sentChanges = new LinkedList<QueuedNotifications>();
    protected QueuedNotifications changes = new QueuedNotifications(1);
    private PushChannel pushChannel;
    private boolean unregistered;
    private final Map<String, DelegateSession> delegateSessions = new HashMap<String, DelegateSession>(3);
    private List<RemoteSessionInfo> remoteSessions;
    private final boolean asAdmin;
    private boolean isOffline = false;
    private final SoapProtocol responseProtocol;
    private String curWaitSetID;
    protected AuthToken authToken;
    protected String originalUserAgent;

    /** Creates a <tt>SoapSession</tt> owned by the given account and
     *  listening on its {@link Mailbox}.
     * @see Session#register() */
    public SoapSession(ZimbraSoapContext zsc) {
        super(zsc.getAuthtokenAccountId(), zsc.getAuthtokenAccountId(), Session.Type.SOAP);
        this.asAdmin = zsc.isUsingAdminPrivileges();
        responseProtocol = zsc.getResponseProtocol();
        curWaitSetID = zsc.getCurWaitSetID();
        userAgent = zsc.getUserAgent();
        requestIPAddress = zsc.getRequestIP();
        authToken = zsc.getAuthToken();
        originalUserAgent = zsc.getOriginalUserAgent();
    }

    @Override
    public SoapSession register() throws ServiceException {
        super.register();

        Mailbox mbox = this.getMailboxOrNull();
        if (mbox != null) {
            recentMessages = mbox.getRecentMessageCount();
            previousAccess = mbox.getLastSoapAccessTime();
            unregistered = false;
            ZimbraLog.session.debug("initializing session recent count to %d", recentMessages);
        }
        return this;
    }

    @Override
    public SoapSession unregister() {
        // when the session goes away, record the timestamp of the last write op to the database
        Mailbox mbox = this.getMailboxOrNull();
        if (lastWrite != -1 && mbox != null) {
            try {
                mbox.recordLastSoapAccessTime(lastWrite);
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.session.warn("exception recording unloaded session's last access time", t);
            }
        }

        // unloading a SoapSession also must unload all its delegates
        List<DelegateSession> delegates;
        synchronized (delegateSessions) {
            delegates = new ArrayList<DelegateSession>(delegateSessions.values());
            delegateSessions.clear();
            unregistered = true;
        }
        for (DelegateSession ds : delegates) {
            ds.unregister();
        }
        // note that Session.unregister() unsets mMailbox...
        super.unregister();
        return this;
    }

    @Override
    protected boolean isMailboxListener() {
        return true;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return true;
    }

    @Override
    protected long getSessionIdleLifetime() {
        return SOAP_SESSION_TIMEOUT_MSEC;
    }

    public boolean isOfflineSoapSession() {
        return isOffline;
    }

    public void setOfflineSoapSession() {
        isOffline = true;
    }

    private boolean asAdmin() {
        return asAdmin;
    }

    public Session getDelegateSession(String targetAccountId) {
        if (unregistered || targetAccountId == null) {
            return null;
        }
        // delegate sessions are only for mailboxes on the local host
        try {
            if (!Provisioning.onLocalServer(Provisioning.getInstance().get(Key.AccountBy.id, targetAccountId))) {
                return null;
            }
        } catch (ServiceException e) {
            ZimbraLog.session.info("exception while fetching delegate session", e);
            return null;
        }

        // catch the case where they ask for a delegate session for the same user
        targetAccountId = targetAccountId.toLowerCase();
        if (targetAccountId.equalsIgnoreCase(mAuthenticatedAccountId)) {
            return this;
        }
        synchronized (delegateSessions) {
            // don't return delegate sessions after an unregister()
            if (unregistered) {
                return null;
            }
            DelegateSession ds = delegateSessions.get(targetAccountId);
            if (ds == null) {
                ds = new DelegateSession(mAuthenticatedAccountId, targetAccountId).register();
                if (ds != null) {
                    delegateSessions.put(targetAccountId, ds);
                }
            }
            return ds;
        }
    }

    void removeDelegateSession(DelegateSession ds) {
        synchronized (delegateSessions) {
            if (unregistered || delegateSessions.isEmpty()) {
                return;
            }
            boolean removed = delegateSessions.remove(ds.mTargetAccountId.toLowerCase()) != null;
            if (!removed) {
                return;
            }
        }
        synchronized (sentChanges) {
            int force = changes.getSequence();
            ZimbraLog.session.debug("removeDelegateSession: changing mForceRefresh: %d -> %d", forceRefresh, force);
            forceRefresh = force;
        }
    }


    public synchronized String getRemoteSessionId(Server server) {
        if (mailbox == null || remoteSessions == null || server == null) {
            return null;
        }
        for (RemoteSessionInfo rsi : remoteSessions) {
            if (rsi.mServerId.equals(server.getId())) {
                return rsi.mSessionId;
            }
        }
        return null;
    }

    protected boolean registerRemoteSessionId(Server server, String sessionId) {
        if (mailbox == null || server == null || sessionId == null) {
            return true;
        }
        String serverId = server.getId().toLowerCase();
        synchronized (this) {
            boolean isNewEntry = true;
            if (remoteSessions == null) {
                remoteSessions = new LinkedList<RemoteSessionInfo>();
            } else {
                for (Iterator<RemoteSessionInfo> it = remoteSessions.iterator(); it.hasNext(); ) {
                    if (it.next().mServerId.equals(server.getId())) {
                        it.remove();
                        isNewEntry = false;
                    }
                }
            }
            remoteSessions.add(new RemoteSessionInfo(sessionId, serverId, System.currentTimeMillis()));
            return isNewEntry;
        }
    }

    public void handleRemoteNotifications(Server server, Element context) {
        handleRemoteNotifications(server, context, false, false);
    }

    protected void handleRemoteNotifications(Server server, Element context, boolean ignoreRefresh, boolean isPing) {
        if (context == null) {
            return;
        }
        boolean refreshExpected = true;

        // remember the remote session ID for the server
        Element eSession = context.getOptionalElement(HeaderConstants.E_SESSION);
        boolean isSoap = eSession != null && eSession.getAttribute(HeaderConstants.A_TYPE, null) == null;
        String sessionId = eSession == null ? null : eSession.getAttribute(HeaderConstants.A_ID, null);
        if (isSoap && sessionId != null && !sessionId.equals("")) {
            refreshExpected = registerRemoteSessionId(server, sessionId);
        }
        // remote refresh should cause overall refresh
        if (!ignoreRefresh && !refreshExpected && context.getOptionalElement(ZimbraNamespace.E_REFRESH) != null) {
            int force = getCurrentNotificationSequence();
            ZimbraLog.session.debug("handleRemoteNotifications: changing mForceRefresh: %d -> %d", forceRefresh, force);
            forceRefresh = force;
        }

        Element eNotify = context.getOptionalElement(ZimbraNamespace.E_NOTIFY);
        if (eNotify != null) {
            RemoteNotifications rns = new RemoteNotifications(eNotify);
            synchronized (sentChanges) {
                if (!skipNotifications(rns.getScaledNotificationCount(), !isPing)) {
                    addRemoteNotifications(rns);
                }
            }
        }
    }

    public void addRemoteNotifications(RemoteNotifications rns) {
        changes.addNotification(rns);
    }

    private void pingRemoteSessions(ZimbraSoapContext zsc) {
        long now = System.currentTimeMillis();

        List<RemoteSessionInfo> needsPing = null;
        synchronized (this) {
            if (remoteSessions == null) {
                return;
            }
            long cutoff = now - getSessionIdleLifetime() / 2;
            for (RemoteSessionInfo rsi : remoteSessions) {
                if (rsi.mLastRequest < cutoff && now - rsi.mLastFailedPing > MINIMUM_PING_RETRY_TIME) {
                    if (needsPing == null) {
                        needsPing = new LinkedList<RemoteSessionInfo>();
                    }
                    needsPing.add(rsi);
                }
            }
        }

        if (needsPing == null) {
            return;
        }
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
        return recentMessages;
    }

    /** Returns the time (in milliseconds) of last write op from any SOAP
     *  session <u>before</u> this session was initiated.  This value remains
     *  constant for the duration of this session. */
    public long getPreviousSessionTime() {
        return previousAccess;
    }

    /** Returns the time (in milliseconds) of the last write operation
     *  initiated by this session.  If the session has not done any write ops
     *  yet, returns {@link #getPreviousSessionTime()}. */
    public long getLastWriteAccessTime() {
        return lastWrite == -1 ? previousAccess : lastWrite;
    }


    @Override
    public void doEncodeState(Element parent) {
        if (pushChannel != null) {
            parent.addAttribute("push", true);
        }
    }


    /** A callback interface which is listening on this session and waiting
     *  for new notifications */
    public static interface PushChannel {
        public void closePushChannel();
        public int getLastKnownSequence();
        public ZimbraSoapContext getSoapContext();
        public boolean localChangesOnly();
        public boolean isPersistent();
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
    public synchronized RegisterNotificationResult registerNotificationConnection(final PushChannel sc)
            throws ServiceException {
        // don't have to lock the Mailbox before locking the Session to avoid deadlock because we're not calling any ToXML functions
        if (pushChannel != null) {
            pushChannel.closePushChannel();
            pushChannel = null;
        }

        // if the session is mailbox listener, then
        // the mailbox variable needs to be set.
        // RemoteSoapSession is not a mailbox listener
        // and does not require mailbox object as
        // the DelegateSession's listen for changes
        // in other's mailboxes.
        if (isMailboxListener() && mailbox == null) {
            sc.closePushChannel();
            return RegisterNotificationResult.NO_NOTIFY;
        }

        boolean dataReady;
        synchronized (sentChanges) {
            // are there any notifications already pending given the passed-in seqno?
            int lastSeqNo = sc.getLastKnownSequence();
            dataReady = changes.hasNotifications(sc.localChangesOnly());
            if (!dataReady && changes.getSequence() > lastSeqNo + 1 && !sentChanges.isEmpty()) {
                for (QueuedNotifications ntfn : sentChanges) {
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
            pushChannel = sc;
            return RegisterNotificationResult.BLOCKING;
        }
    }


    @Override
    public void notifyExternalEvent(ExternalEventNotification extra) {
        if (extra == null) {
            return;
        }
        synchronized (sentChanges) {
            changes.addNotification(extra);
        }
        try {
            // if we're in a hanging no-op, alert the client that there are changes
            notifyPushChannel(null);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in notifyExternalEvent", e);
        }
    }

    /** On the session's first write op, record the timestamp to the database. */
    public void updateLastWrite(Mailbox mbox) {
        boolean firstWrite = lastWrite == -1;
        lastWrite = System.currentTimeMillis();
        if (firstWrite) {
            try {
                mbox.recordLastSoapAccessTime(lastWrite);
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
     * @param changeId  The change ID of the change.
     * @param source    The (optional) Session which initiated these changes. */
    @Override
    public void notifyPendingChanges(PendingModifications pmsIn, int changeId, Session source) {
        PendingLocalModifications pms = (PendingLocalModifications) pmsIn;
        Mailbox mbox = this.getMailboxOrNull();
        if (pms == null || mbox == null || !pms.hasNotifications()) {
            return;
        }
        if (source == this) {
            updateLastWrite(mbox);
        } else {
            // keep track of "recent" message count: all present before the session started, plus all received during the session
            if (pms.created != null) {
                for (BaseItemInfo item : pms.created.values()) {
                    if (item instanceof Message) {
                        Message msg = (Message) item;
                        boolean isReceived = true;
                        if (msg.getFolderId() == Mailbox.ID_FOLDER_SPAM || msg.getFolderId() == Mailbox.ID_FOLDER_TRASH) {
                            isReceived = false;
                        } else if ((item.getFlagBitmask() & Mailbox.NON_DELIVERY_FLAGS) != 0) {
                            isReceived = false;
                        } else if (source != null) {
                            isReceived = false;
                        }
                        if (isReceived) {
                            recentMessages++;
                            ZimbraLog.session.debug("incrementing session recent count to %d", recentMessages);
                        }
                    }
                }
            }
        }

        handleNotifications(pms, source == this);
    }

    boolean hasSerializableChanges(PendingLocalModifications pms) {
        // catch cases where the only notifications are mailbox config changes, which we don't serialize
        if (pms.created != null && !pms.created.isEmpty()) {
            return true;
        }
        if (pms.deleted != null && !pms.deleted.isEmpty()) {
            return true;
        }
        if (pms.modified != null && !pms.modified.isEmpty()) {
            for (Change chg : pms.modified.values()) {
                if (!(chg.what instanceof Mailbox) || chg.why != Change.CONFIG) {
                    return true;
                }
            }
        }
        return false;
    }

    void handleNotifications(PendingLocalModifications pms, boolean fromThisSession) {
        if (!hasSerializableChanges(pms)) {
            return;
        }
        try {
            // update the set of notifications not yet sent to the client
            cacheNotifications(pms, fromThisSession);
            // if we're in a hanging no-op, alert the client that there are changes
            notifyPushChannel(pms);
            // FIXME: this query result cache purge seems a little aggressive
            clearCachedQueryResults();
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in notifyPendingChanges ", e);
        }
    }

    private void cacheNotifications(PendingLocalModifications pms, boolean fromThisSession) {
        // XXX: should constrain to folders, tags, and stuff relevant to the current query?

        synchronized (sentChanges) {
            if (!skipNotifications(pms.getScaledNotificationCount(), fromThisSession)) {
                // if we're here, these changes either
                //   a) do not cause the session's notification cache to overflow, or
                //   b) originate from this session and hence must be notified back to the session
                changes.addNotification(pms);
            }
        }
    }

    protected boolean skipNotifications(int notificationCount, boolean fromThisSession) {
        // if we're going to be sending a <refresh> anyway, there's no need to record these changes
        int currentSequence = getCurrentNotificationSequence();
        if (forceRefresh == currentSequence && !fromThisSession) {
            return true;
        }
        // determine whether this set of notifications would cause the cached set to overflow
        if (forceRefresh != currentSequence && MAX_QUEUED_NOTIFICATIONS > 0) {
            // XXX: more accurate would be to combine pms and mChanges and take the count...
            int count = notificationCount + changes.getScaledNotificationCount();
            if (count > MAX_QUEUED_NOTIFICATIONS) {
                // if we've overflowed, jettison the pending change set
                changes.clearMailboxChanges();
                int force = currentSequence;
                ZimbraLog.session.debug("skipNotifications: changing mForceRefresh: %d -> %d", forceRefresh, force);
                forceRefresh = force;
            }

            for (QueuedNotifications ntfn : sentChanges) {
                count += ntfn.getScaledNotificationCount();
                if (count > MAX_QUEUED_NOTIFICATIONS) {
                    ntfn.clearMailboxChanges();
                    int force = Math.max(forceRefresh, ntfn.getSequence());
                    ZimbraLog.session.debug("skipNotifications: changing mForceRefresh: %d -> %d", forceRefresh, force);
                    forceRefresh = force;
                }
            }
        }

        return (forceRefresh == currentSequence && !fromThisSession);
    }

    public void forcePush() {
        try {
            notifyPushChannel(null, false);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("ServiceException in forcePush", e);
        }
    }

    private synchronized void notifyPushChannel(final PendingLocalModifications pms) throws ServiceException {
        // don't clear the persistent push channels after each use
        boolean persistent = pushChannel == null ? false : pushChannel.isPersistent();
        notifyPushChannel(pms, !persistent);
    }

    private synchronized void notifyPushChannel(final PendingLocalModifications pms, final boolean clearChannel)
            throws ServiceException {
        // don't have to lock the Mailbox before locking the Session to avoid deadlock because we're not calling any ToXML functions
        if (pushChannel == null) {
            return;
        }
        // ignore the notification if we're only interested in local changes and these aren't local
        if (pushChannel.localChangesOnly() && pms != null && !pms.overlapsWithAccount(mAuthenticatedAccountId)) {
            return;
        }
        pushChannel.notificationsReady();
        if (clearChannel) {
            pushChannel = null;
        }
    }


    public boolean requiresRefresh(final int lastSequence) {
        synchronized (sentChanges) {
            boolean required = false;
            int currentSeq = getCurrentNotificationSequence();
            if (lastSequence <= 0) {
                required = forceRefresh == currentSeq;
            } else {
                required = forceRefresh > Math.min(lastSequence, currentSeq);
            }
            ZimbraLog.session.debug("refresh required: forceRefresh=%d,lastSequence=%d,currentSequence=%d",
                    forceRefresh, lastSequence, currentSeq);
            return required;
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
        Mailbox mbox = this.getMailboxOrNull();
        if (mbox == null) {
            return;
        }
        synchronized (sentChanges) {
            for (QueuedNotifications ntfn : sentChanges) {
                ntfn.clearMailboxChanges();
            }
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
                if (tag != null && !(tag instanceof Flag)) {
                    ToXML.encodeTag(eTags, ifmt, octxt, tag);
                }
            }
        }

        // first, get the user's folder hierarchy
        FolderNode root = mbox.getFolderTree(octxt, null, false);
        OperationContextData.setNeedGranteeName(octxt, false);
        GetFolder.encodeFolderNode(root, eRefresh, ifmt, octxt);

        // The Boolean of the Pair indicates whether the mountpoint is found to be broken
        Map<ItemId, Pair<Boolean, Element>> mountpoints = new HashMap<ItemId, Pair<Boolean, Element>>();
        // for mountpoints pointing to this host, get the serialized folder subhierarchy
        expandLocalMountpoints(octxt, root, eRefresh.getFactory(), mountpoints);
        // for mountpoints pointing to other hosts, get the folder structure from the remote server
        expandRemoteMountpoints(octxt, zsc, mountpoints);

        // graft in subfolder trees from the other user's mailbox, making sure that mountpoints reflect the counts (etc.) of the target folder
        if (!mountpoints.isEmpty()) {
            transferMountpointContents(eRefresh.getOptionalElement(MailConstants.E_FOLDER), octxt, mountpoints);
        }
    }

    private void expandLocalMountpoints(OperationContext octxt, FolderNode node, Element.ElementFactory factory,
            Map<ItemId, Pair<Boolean, Element>> mountpoints) {
        if (node.mFolder == null || mountpoints == null) {
            return;
        } else if (node.mFolder instanceof Mountpoint) {
            Mountpoint mpt = (Mountpoint) node.mFolder;
            expandLocalMountpoint(octxt, mpt, factory, mountpoints);
        } else {
            for (FolderNode child : node.mSubfolders) {
                expandLocalMountpoints(octxt, child, factory, mountpoints);
            }
        }
    }

    private void expandLocalMountpoint(OperationContext octxt, Mountpoint mpt, Element.ElementFactory factory,
            Map<ItemId, Pair<Boolean, Element>> mountpoints) {
        // don't bother generating the subhierarchy more than once
        ItemId iidTarget = mpt.getTarget();
        if (mountpoints.containsKey(iidTarget)) {
            return;
        }
        try {
            Provisioning prov = Provisioning.getInstance();
            Account owner = prov.get(Key.AccountBy.id, mpt.getOwnerId(), octxt.getAuthToken());
            if (owner == null || owner.getId().equals(mAuthenticatedAccountId)) {
                mountpoints.put(iidTarget, new Pair<Boolean, Element>(true, null));
                return;
            }

            // treat the target account as inactive if it's in maintenance mode, or
            // if we're non-admin and it's not active
            if (Provisioning.ACCOUNT_STATUS_MAINTENANCE.equals(owner.getAccountStatus(prov)) ||
                    (!Provisioning.ACCOUNT_STATUS_ACTIVE.equals(owner.getAccountStatus(prov)) &&
                            (!octxt.isUsingAdminPrivileges() ||
                                    !AccessManager.getInstance().canAccessAccount(octxt.getAuthenticatedUser(), owner)))) {
                mountpoints.put(iidTarget, new Pair<Boolean, Element>(true, null));
                return;
            }

            // handle mountpoints pointing to a different server later
            if (!Provisioning.onLocalServer(owner)) {
                mountpoints.put(iidTarget, null);
                return;
            }

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);
            FolderNode remote = mbox.getFolderTree(octxt, new ItemId(mbox, mpt.getRemoteId()), false);

            if (remote != null && remote.mFolder != null && !remote.mFolder.isHidden()) {
                ItemIdFormatter ifmt = new ItemIdFormatter(octxt.getAuthenticatedUser(), mbox, false);
                if (OperationContextData.getNeedGranteeName(octxt)) {
                    OperationContextData.addGranteeNames(octxt, remote);
                }
                Element subhierarchy = GetFolder.encodeFolderNode(remote, factory.createElement("ignored"), ifmt, octxt).detach();
                mountpoints.put(iidTarget, new Pair<Boolean, Element>(false, subhierarchy));
                // fault in a delegate session because there's actually something to listen on...
                getDelegateSession(mpt.getOwnerId());
            }
        } catch (ServiceException e) {
            mountpoints.put(iidTarget, new Pair<Boolean, Element>(true, null));
        }
    }

    private void expandRemoteMountpoints(OperationContext octxt, ZimbraSoapContext zsc,
            Map<ItemId, Pair<Boolean, Element>> mountpoints) {
        Map<String, Server> remoteServers = null;
        Provisioning prov = Provisioning.getInstance();
        for (Map.Entry<ItemId, Pair<Boolean, Element>> mptinfo : mountpoints.entrySet()) {
            try {
                // local mountpoints already have their targets serialized
                if (mptinfo.getValue() != null) {
                    continue;
                }
                Account owner = prov.get(Key.AccountBy.id, mptinfo.getKey().getAccountId(), zsc.getAuthToken());
                if (owner == null) {
                    continue;
                }
                Server server = prov.getServer(owner);
                if (server == null) {
                    continue;
                }
                if (remoteServers == null) {
                    remoteServers = new HashMap<String, Server>(3);
                }
                remoteServers.put(owner.getId(), server);
            } catch (ServiceException e) {
            }
        }

        if (remoteServers != null && !remoteServers.isEmpty()) {
            Map<String, Element> remoteHierarchies = fetchRemoteHierarchies(octxt, zsc, remoteServers);
            for (Map.Entry<ItemId, Pair<Boolean, Element>> mptinfo : mountpoints.entrySet()) {
                // local mountpoints already have their targets serialized
                if (mptinfo.getValue() != null) {
                    continue;
                }
                ItemId iid = mptinfo.getKey();
                Element remoteFolderElement =
                        findRemoteFolder(iid.toString(mAuthenticatedAccountId), remoteHierarchies.get(iid.getAccountId()));
                mptinfo.setValue(new Pair<Boolean, Element>(remoteFolderElement == null, remoteFolderElement));
            }
        }
    }

    private Map<String, Element> fetchRemoteHierarchies(OperationContext octxt, ZimbraSoapContext zsc, Map<String, Server> remoteServers) {
        Map<String, Element> hierarchies = new HashMap<String, Element>();

        Element noop;
        try {
            noop = Element.create(zsc.getRequestProtocol(), MailConstants.GET_FOLDER_REQUEST).addAttribute(MailConstants.A_VISIBLE, true);
            if (!OperationContextData.getNeedGranteeName(octxt)) {
                noop.addAttribute(MailConstants.A_NEED_GRANTEE_NAME, false);
            }
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
            } catch (ServiceException e) {
            }
        }
        return hierarchies;
    }

    private static Element findRemoteFolder(String id, Element eFolder) {
        if (id == null || eFolder == null) {
            return null;
        }
        if (id.equalsIgnoreCase(eFolder.getAttribute(MailConstants.A_ID, null))) {
            // folder stubs (hierarchy placeholders for non-readable folders) contain only "id" and "name"
            boolean isStub = eFolder.getAttribute(MailConstants.A_SIZE, null) == null;
            return isStub ? null : eFolder.clone();
        }

        for (Element eSubfolder : eFolder.listElements()) {
            Element match = findRemoteFolder(id, eSubfolder);
            if (match != null) {
                // Assemble the absolute folder path in remote account. This will be used for calculating REST url.
                String folderPath = match.getAttribute(MailConstants.A_ABS_FOLDER_PATH, null);
                if (folderPath == null) {
                    folderPath = match.getAttribute(MailConstants.A_NAME, null);
                }
                if (folderPath != null && !folderPath.startsWith("/")) {
                    String parentFolderName = eFolder.getAttribute(MailConstants.A_NAME, null);
                    if (parentFolderName != null) {
                        String newPath;
                        if (parentFolderName.equals("USER_ROOT")) {
                            newPath = "/" + folderPath;
                        } else {
                            newPath = parentFolderName + "/" + folderPath;
                        }
                        match.addAttribute(MailConstants.A_ABS_FOLDER_PATH, newPath);
                    }
                }
                return match;
            }
        }
        return null;
    }

    private static void transferMountpointContents(Element elem, OperationContext octxt,
            Map<ItemId, Pair<Boolean, Element>> mountpoints)
            throws ServiceException {
        if (elem == null) {
            return;
        }
        Pair<Boolean, Element> target = null;
        if (elem.getName().equals(MailConstants.E_MOUNT)) {
            ItemId iidTarget = new ItemId(elem.getAttribute(MailConstants.A_ZIMBRA_ID, null), (int) elem.getAttributeLong(MailConstants.A_REMOTE_ID, -1));
            target = mountpoints.get(iidTarget);
        }

        if (target == null) {
            // not a local mountpoint with a known target; recurse on children
            for (Element child : elem.listElements()) {
                transferMountpointContents(child, octxt, mountpoints);
            }
        } else {
            boolean broken = target.getFirst();
            if (broken) {
                elem.addAttribute(MailConstants.A_BROKEN, true);
            } else {
                ToXML.transferMountpointContents(elem, target.getSecond());
            }
        }
    }


    public int getCurrentNotificationSequence() {
        synchronized (sentChanges) {
            return changes.getSequence();
        }
    }

    public void acknowledgeNotifications(int sequence) {
        synchronized (sentChanges) {
            if (sentChanges == null || sentChanges.isEmpty()) {
                return;
            }
            if (sequence <= 0) {
                // if the client didn't send a valid "last sequence number", *don't* keep old changes around
                sentChanges.clear();
            } else {
                // clear any notifications we now know the client has received
                for (Iterator<QueuedNotifications> it = sentChanges.iterator(); it.hasNext(); ) {
                    if (it.next().getSequence() <= sequence) {
                        it.remove();
                    } else {
                        break;
                    }
                }
            }
        }
    }

    public Collection<PendingLocalModifications> getNotifications() {
        List<PendingLocalModifications> ret = new ArrayList<PendingLocalModifications>();
        synchronized (sentChanges) {
            for (QueuedNotifications notification : sentChanges) {
                if (notification.hasNotifications()) {
                    ret.add(notification.mMailboxChanges);
                }
            }
        }
        return ret;
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
     * @return The passed-in <tt>&lt;context></tt> element
     * @throws ServiceException */
    public Element putNotifications(Element ctxt, ZimbraSoapContext zsc, int lastSequence) throws ServiceException {
        Mailbox mbox = this.getMailboxOrNull();
        if (ctxt == null || mbox == null) {
            return null;
        }
        if (Provisioning.getInstance().allowsPingRemote()) {
            // if there are remote sessions that haven't been accessed in a while, ping them
            pingRemoteSessions(zsc);
        }

        // because ToXML functions can now call back into the Mailbox, don't hold any locks when calling putQueuedNotifications
        LinkedList<QueuedNotifications> notifications;
        synchronized (sentChanges) {
            // send the "change" block:  <change token="555"/>
            ctxt.addUniqueElement(HeaderConstants.E_CHANGE).addAttribute(HeaderConstants.A_CHANGE_ID, mbox.getLastChangeID());

            // clear any notifications we now know the client has received
            acknowledgeNotifications(lastSequence);

            // cover ourselves in case a client is doing something really stupid...
            if (sentChanges.size() > 20) {
                ZimbraLog.session.warn("clearing abnormally long notification change list due to misbehaving client");
                sentChanges.clear();
            }

            if (changes.hasNotifications() || requiresRefresh(lastSequence)) {
                assert(changes.getSequence() >= 1);
                int newSequence = changes.getSequence() + 1;
                sentChanges.add(changes);
                changes = new QueuedNotifications(newSequence);
            }

            // mChanges must be empty at this point (everything moved into the mSentChanges list)
            assert(!changes.hasNotifications());

            // drop out if notify is off or if there is nothing to send
            if (sentChanges.isEmpty()) {
                return ctxt;
            }
            notifications = new LinkedList<QueuedNotifications>(sentChanges);
        }

        // send all the old changes
        QueuedNotifications last = notifications.getLast();
        for (QueuedNotifications ntfn : notifications) {
            if (ntfn.hasNotifications() || ntfn == last) {
                putQueuedNotifications(mbox, ntfn, ctxt, zsc);
            }
        }

        return ctxt;
    }

    /** Size limit beyond which we suppress notifications on conversations
     *  belonging to other people's mailboxes.  We need to fetch the entire
     *  list of visible messages when serializing delegated conversations.
     *  If it looks like it'd be too expensive to fetch that list, we just
     *  skip the notification. */
    private static final int DELEGATED_CONVERSATION_SIZE_LIMIT = 50;

    protected static final String A_ID = HeaderConstants.A_ID;

    private boolean encodingMatches(Element parent, Element newChild) {
        return parent.getClass().equals(newChild.getClass());
    }

    /** Write a single instance of the PendingLocalModifications structure into the
     *  passed-in <ctxt> block.
     * @throws ServiceException */
    protected void putQueuedNotifications(Mailbox mbox, QueuedNotifications ntfn, Element parent, ZimbraSoapContext zsc) throws ServiceException {
        // create the base "notify" block:  <notify seq="6"/>
        Element eNotify = parent.addNonUniqueElement(ZimbraNamespace.E_NOTIFY);
        if (ntfn.getSequence() > 0) {
            eNotify.addAttribute(HeaderConstants.A_SEQNO, ntfn.getSequence());
        }
        OperationContext octxt = null;
        try {
            octxt = DocumentHandler.getOperationContext(zsc, this);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("error fetching operation context for: " + zsc.getAuthtokenAccountId(), e);
            return;
        }

        boolean debug = ZimbraLog.session.isDebugEnabled();

        PendingLocalModifications pms = ntfn.mMailboxChanges;
        RemoteNotifications rns = ntfn.mRemoteChanges;

        Element eDeleted = eNotify.addUniqueElement(ZimbraNamespace.E_DELETED);
        StringBuilder deletedIds = new StringBuilder();
        if (pms != null && pms.deleted != null && pms.deleted.size() > 0) {
            for (ModificationKey mkey : pms.deleted.keySet()) {
                addDeletedNotification(mkey, deletedIds);
            }
        }
        if (rns != null && rns.deleted != null) {
            deletedIds.append(deletedIds.length() == 0 ? "" : ",").append(rns.deleted);
        }
        boolean hasLocalCreates = pms != null && pms.created != null && !pms.created.isEmpty();
        boolean hasRemoteCreates = rns != null && rns.created != null && !rns.created.isEmpty();
        boolean hasLocalModifies = pms != null && pms.modified != null && !pms.modified.isEmpty();
        boolean hasRemoteModifies = rns != null && rns.modified != null && !rns.modified.isEmpty();
        if(SoapTransport.NotificationFormat.valueOf(zsc.getNotificationFormat()) == SoapTransport.NotificationFormat.IMAP) {
            try {
                AccountWithModifications info = new AccountWithModifications(zsc.getAuthtokenAccountId(), mbox.getLastChangeID());
                Map<Integer, PendingFolderModifications> folderMods = PendingModifications.encodeIMAPFolderModifications(pms);
                info.setPendingFolderModifications(folderMods.values());
                eNotify.addUniqueElement(JaxbUtil.jaxbToElement(info, eNotify.getFactory()));
            } catch (ContainerException | ServiceException e) {
                ZimbraLog.session.error("Failed to encode IMAP notifications for a SOAP session ", e);
            }
        }
        if (hasLocalCreates || hasRemoteCreates) {
            Element eCreated = eNotify.addUniqueElement(ZimbraNamespace.E_CREATED);
            if (hasLocalCreates) {
                for (BaseItemInfo item : pms.created.values()) {
                    if (item instanceof MailItem) {
                        MailItem mi = (MailItem) item;
                        ItemIdFormatter ifmt = new ItemIdFormatter(mAuthenticatedAccountId, mi.getMailbox(), false);
                        try {
                            Element elem = ToXML.encodeItem(eCreated, ifmt, octxt, mi, ToXML.NOTIFY_FIELDS);
                            // special-case notifications for new mountpoints in the authenticated user's mailbox
                            if (item instanceof Mountpoint && mbox == mi.getMailbox()) {
                                Map<ItemId, Pair<Boolean, Element>> mountpoints = new HashMap<ItemId, Pair<Boolean, Element>>(2);
                                expandLocalMountpoint(octxt, (Mountpoint) mi, eCreated.getFactory(), mountpoints);
                                expandRemoteMountpoints(octxt, zsc, mountpoints);
                                transferMountpointContents(elem, octxt, mountpoints);
                            }
                            if (item instanceof Message) {
                                Message msg = (Message) item;
                                //change the flag on the cached item; this is just a snapshot
                                mbox.getMessageById(octxt, msg.getId()).advanceEventFlag(EventFlag.seen);
                            }
                        } catch (ServiceException e) {
                            ZimbraLog.session.warn("error encoding item " + mi.getId(), e);
                            return;
                        }
                    }
                }
                // sanity-check the returned element
                if (!eCreated.hasChildren() && debug) {
                    ZimbraLog.session.debug("no serialied creates for item set: %s", pms.created.keySet());
                }
            }
            if (hasRemoteCreates) {
                if (debug) {
                    ZimbraLog.session.debug("adding %d proxied creates", rns.created.size());
                }
                for (Element elt : rns.created) {
                    if (encodingMatches(parent, elt)) {
                        eCreated.addElement(elt.clone().detach());
                    } else {
                        ZimbraLog.session.warn("unable to add remote notification due to mismatched SOAP protocol");
                    }
                }
            }
        }

        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        if (hasLocalModifies || hasRemoteModifies) {
            Element eModified = eNotify.addUniqueElement(ZimbraNamespace.E_MODIFIED);
            if (hasLocalModifies) {
                for (Change chg : pms.modified.values()) {
                    if (chg.why != 0 && chg.what instanceof MailItem) {
                        MailItem item = (MailItem) chg.what;

                        try {
                            Element elt = ToXML.encodeItem(eModified, ifmt, octxt, item, chg.why);
                            if (elt == null) {
                                ModificationKey mkey = new PendingLocalModifications.ModificationKey(item);
                                addDeletedNotification(mkey, deletedIds);
                                if (debug) {
                                    ZimbraLog.session.debug("marking nonserialized item as a delete: %s", mkey);
                                }
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
                if (!eModified.hasChildren() && debug) {
                    ZimbraLog.session.debug("no serialied modifies for item set: %s", pms.modified.keySet());
                }
            }
            if (hasRemoteModifies) {
                if (debug) {
                    ZimbraLog.session.debug("adding %d proxied modifies", rns.modified.size());
                }
                for (Element elt : rns.modified) {
                    if (encodingMatches(parent, elt)) {
                        eModified.addElement(elt.clone().detach());
                    } else {
                        ZimbraLog.session.warn("unable to add remote notification due to mismatched SOAP protocol");
                    }
                }
            }
        }

        if (rns != null && rns.activities != null && !rns.activities.isEmpty())  {
            for (Element elt : rns.activities) {
                if (encodingMatches(parent, elt)) {
                    eNotify.addElement(elt.clone().detach());
                } else {
                    ZimbraLog.session.warn("unable to add remote notification due to mismatched SOAP protocol");
                }
            }
        }

        putExtraNotifications(ntfn, eNotify, ifmt);

        if (deletedIds == null || deletedIds.length() == 0) {
            eDeleted.detach();
        } else {
            eDeleted.addAttribute(A_ID, deletedIds.toString());
        }
    }

    /*
     * Octopus should be eventually merged to ZCS so we don't have to use
     * callbacks as much
     */
    private void putExtraNotifications(QueuedNotifications ntfn, Element eNotify, ItemIdFormatter ifmt) {

        // watch notifications are stored on the side as they are not
        // MailItem based notification
        if (ntfn.mExternalNotifications != null && ntfn.mExternalNotifications.size() > 0) {
            for (ExternalEventNotification extra : ntfn.mExternalNotifications) {
                extra.addElement(eNotify);
            }
        }

        // activities
        if (activityCb != null && ntfn.mMailboxChanges != null) {
            try {
                activityCb.putActivities(ntfn.mMailboxChanges, eNotify, ifmt);
            } catch (ServiceException e) {
                ZimbraLog.soap.warn("logging activities", e);
            }
        }
    }

    public interface ActivityCallback {
        public void putActivities(PendingLocalModifications pms, Element notify, ItemIdFormatter ifmt) throws ServiceException;
    }

    private static ActivityCallback activityCb;

    public static void setActivityCallback(ActivityCallback cb) {
        activityCb = cb;
    }

    private void addDeletedNotification(ModificationKey mkey, StringBuilder deletedIds) {
        if (deletedIds.length() != 0) {
            deletedIds.append(',');
        }
        // should be using the ItemIdFormatter, but I'm preoptimizing here
        if (!mkey.getAccountId().equals(mAuthenticatedAccountId)) {
            deletedIds.append(mkey.getAccountId()).append(':');
        }
        deletedIds.append(mkey.getItemId());
    }


    public synchronized void clearCachedQueryResults() {
        try {
            IOUtil.closeQuietly(queryResults);
        } finally {
            queryString = "";
            groupBy  = "";
            sortBy   = "";
            queryResults = null;
        }
    }

    public synchronized void putQueryResults(String query, String groupBy, String sortBy, ZimbraQueryResults res) {
        clearCachedQueryResults();
        this.queryString = query;
        this.groupBy = groupBy;
        this.sortBy = sortBy;
        this.queryResults = res;
    }

    public synchronized ZimbraQueryResults getQueryResults(String query, String groupBy, String sortBy) {
        if (queryString.equals(query) && this.groupBy.equals(groupBy) && this.sortBy.equals(sortBy)) {
            return queryResults;
        } else {
            return null;
        }
    }

    public SoapProtocol getResponseProtocol() {
        return responseProtocol;
    }

    @Override
    public void cleanup() {
        clearCachedQueryResults();
    }

    public void setCurWaitSetID(String waitSetID) {
        curWaitSetID = waitSetID;
    }

    public String getCurWaitSetID() {
        return curWaitSetID;
    }

    public AuthToken getAuthToken() {
        return authToken;
    }

    public String getOriginalUserAgent() {
        return originalUserAgent;
    }
}
