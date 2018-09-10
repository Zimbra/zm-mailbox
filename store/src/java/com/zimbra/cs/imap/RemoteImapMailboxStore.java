/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZTag;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.InputStreamWithSize;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.mail.type.ImapMessageInfo;

public class RemoteImapMailboxStore extends ImapMailboxStore {

    private transient ZMailbox zMailbox;
    private transient String accountId;

    public RemoteImapMailboxStore(ZMailbox mailbox) throws ServiceException {
        super();
        this.zMailbox = mailbox;
        this.accountId = mailbox.getAccountId();
    }

    public RemoteImapMailboxStore(ZMailbox mailbox, String accountId) {
        super();
        this.zMailbox = mailbox;
        this.accountId = accountId;
    }

    @Override
    public ImapListener createListener(ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        ImapRemoteSession irs = new ImapRemoteSession(this, i4folder, handler);
        /* Without this, we may end up waiting for WaitSets to finish, expecting them to have
         * spotted a change because the WaitSet last known change will be 0 but we want to be at least
         * as up to date as the associated ZMailbox.  At this point, our view should be at least as
         * up to date as ZMailbox, so this shouldn't cause loss of updates.
         */
        irs.updateLastChangeId(irs.getMailbox().getLastChangeID());
        return irs;
    }
    
    @Override
    public List<ImapListener> getListeners(ItemIdentifier ident) {
        String acctId = ident.accountId != null ? ident.accountId : getAccountId();
        try {
            ImapServerListener listener = ImapServerListenerPool.getInstance().getForAccountId(acctId);
            return Lists.newArrayList(listener.getListeners(acctId, ident.id));
        } catch (ServiceException se) {
            ZimbraLog.imap.debug("Problem getting listeners for folder=%s acct=%s from ImapServerListener",
                    ident, acctId, se);
            return Collections.emptyList();
        }
    }

    @Override
    public ImapFlag getTagByName(String tag) throws ServiceException {
        ZTag ztag = zMailbox.getTagByName(tag);
        return ztag == null ? null : new ImapFlag(ztag);
    }

    @Override
    public void resetImapUid(List<Integer> renumber) throws ServiceException {
        zMailbox.resetImapUid(renumber);
    }

    @Override
    public void saveSubscriptions(OperationContext octxt, Set<String> subs) throws ServiceException {
        zMailbox.saveIMAPsubscriptions(subs);
    }

    @Override
    public void beginTrackingImap() throws ServiceException {
        zMailbox.beginTrackingImap();
    }

    @Override
    public void deleteMessages(OperationContext octxt, List<Integer> ids) {
        for (int id : ids) {
            try {
                    zMailbox.deleteMessage(String.valueOf(id));
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("failed to delete message: %s", id);
            }
        }
    }

    /**
     * MUST only be called when the source items and target folder are in the same mailbox
     * @return List of IMAP UIDs
     */
    @Override
    public List<Integer> imapCopy(OperationContext octxt, int[] itemIds, MailItemType type, int folderId)
            throws IOException, ServiceException {
        return zMailbox.imapCopy(itemIds, type, folderId);
    }

    @Override
    public InputStreamWithSize getByImapId(OperationContext octxt, int imapId, String folderId, String resolvedPath)
    throws ServiceException {
        AuthToken auth;
        try {
            auth = AuthToken.getAuthToken(zMailbox.getAuthToken().getValue());
        } catch (AuthTokenException ate) {
            ZimbraLog.imap.error("Problem with auth token", ate);
            throw ServiceException.AUTH_EXPIRED("Problem creating auth token for use with UserServlet");
        }
        HashMap<String, String> params = Maps.newHashMapWithExpectedSize(1);
        params.put(UserServlet.QP_IMAP_ID, Integer.toString(imapId));
        UserServlet.HttpInputStream is;
        try {
            is = UserServlet.getRemoteContentAsStream(auth, getAccount(), resolvedPath, params);
            return new InputStreamWithSize(is, (long) is.getContentLength());
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("Failed to get content stream for item id %d",  imapId), e);
        }
    }

    @Override
    public void checkAppendMessageFlags(OperationContext octxt, List<AppendMessage> appends) throws ServiceException {
        ImapFlagCache flagset = ImapFlagCache.getSystemFlags();
        ImapFlagCache tagset = new ImapFlagCache(zMailbox);
        for (AppendMessage append : appends) {
            append.checkFlags(flagset, tagset);
        }
    }

    @Override
    public int getCurrentMODSEQ(ItemIdentifier folderId) throws ServiceException {
        return zMailbox.getFolderById(folderId.toString()).getImapMODSEQ();
    }

    @Override
    public Collection<FolderStore> getVisibleFolders(OperationContext octxt, ImapCredentials credentials,
            String owner, ImapPath relativeTo)
    throws ServiceException {
        String root = relativeTo == null ? "" : "/" + relativeTo.asResolvedPath();
        List<ZFolder> allZFolders = zMailbox.getAllFolders();
        Set<FolderStore> fStores = Sets.newHashSetWithExpectedSize(allZFolders.size());
        List<ZFolder> zfolders = null;
        zfolders = zMailbox.getAllFolders();
        for (ZFolder zfolder : zfolders) {
            if (!zfolder.getPath().startsWith(root) || zfolder.getPath().equals(root)) {
                continue;
            }
            ImapPath path;
            path = (relativeTo == null) ? new ImapPath(owner, zfolder, credentials)
                    : new ImapPath(owner, zfolder, relativeTo);
            if (path.isVisible()) {
                fStores.add(zfolder);
            }
        }
        return fStores;
    }

    @Override
    public boolean addressMatchesAccountOrSendAs(String givenAddress) throws ServiceException {
        return (AccountUtil.addressMatchesAccountOrSendAs(getAccount(), givenAddress));
    }

    @Override
    public int getImapRECENTCutoff(FolderStore folder) {
        boolean isWritable = false;
        for (ImapListener imapListener: getListeners(folder)) {
            if (imapListener.isWritable()) {
                isWritable = true;
                break;
            }
        }
        return ((ZFolder) folder).getImapRECENTCutoff(isWritable);
    }

    /**
     * Returns the number of messages in the folder that would be considered \Recent in an IMAP session.
     * If there is currently a READ-WRITE IMAP session open on the folder, by definition all other IMAP connections
     *  will see no \Recent messages.  <i>(Note that as such, this method should <u>not</u> be called by IMAP sessions
     *  that have this folder selected.)</i>  Otherwise, it is the number of messages/chats/contacts added to the
     *  folder, moved to the folder, or edited in the folder since the last such IMAP session.
     */
    @Override
    public int getImapRECENT(OperationContext octxt, FolderStore folder) throws ServiceException {
        /* The equivalent Local case ends up in Folder.getImapRECENT() which loops over sessions
         * to see if the folder is in use anywhere.
         * The implementation of this needs to have a similar notion of whether the folder is in
         * use anywhere. (Interesting other thought.  Does it need to be an IMAP session to qualify?
         * If the folder is seen by some other SOAP client or a Sync client, doesn't that invalidate
         * the recency test just as much as another IMAP client?  So, could consider all listeners
         * here AND on the mailbox??  Thinking that just remote listeners may not cut it)
         */
        for (ImapListener imapListener: getListeners(folder)) {
            if (imapListener.isWritable())
            {
                ZimbraLog.imap.debug("getImapRECENT found listener %s", imapListener);
                return 0;
            }
        }
        return ((ZFolder) folder).getImapRECENT();
    }

    @Override
    public MailboxStore getMailboxStore() {
        return zMailbox;
    }

    @Override
    public Account getAccount() throws ServiceException {
        return Provisioning.getInstance().get(AccountBy.id, accountId);
    }

    /** Returns the ID of this mailbox's Account. */
    @Override
    public String getAccountId() {
        return accountId;
    }

    public ZMailbox getZMailbox() {
        return zMailbox;
    }

    @Override
    public Set<String> listSubscriptions(OperationContext octxt) throws ServiceException {
        Set<String> subs = zMailbox.listIMAPSubscriptions();
        if(subs != null && !subs.isEmpty()) {
            return subs;
        } else {
            return null;
        }
    }

    @Override
    public List<ImapMessage> openImapFolder(OperationContext octxt, ItemIdentifier folderId) throws ServiceException {
        List<ImapMessage> msgs = new ArrayList<ImapMessage>();
        Integer chunkSize = Provisioning.getInstance().getLocalServer().getOpenImapFolderRequestChunkSize();
        for (ImapMessageInfo msg: zMailbox.openImapFolder(folderId, chunkSize)) {
            msgs.add(new ImapMessage(msg));
        }
        return msgs;
    }

    @Override
    public void registerWithImapServerListener(ImapListener listener) {
        ImapServerListener svrListener;
        try {
            svrListener = ImapServerListenerPool.getInstance().get(zMailbox);
            if (null != svrListener) {
                svrListener.addListener((ImapRemoteSession)listener);
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.info("Problem registering with ImapServerListener", e);
        }
    }

    @Override
    public void unregisterWithImapServerListener(ImapListener listener) {
        try {
            ImapServerListener svrListener = ImapServerListenerPool.getInstance().get(zMailbox);
            if (null != svrListener) {
                svrListener.removeListener((ImapRemoteSession)listener);
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.info("Problem unregistering with ImapServerListener", e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("accId", accountId).add("mbox", zMailbox).toString();
    }
}
