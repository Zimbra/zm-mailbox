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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.InputStreamWithSize;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;

public class LocalImapMailboxStore extends ImapMailboxStore {

    private transient Mailbox mailbox;


    public LocalImapMailboxStore(Mailbox mailbox) {
        super();
        this.mailbox = mailbox;
    }

    @Override
    public ImapListener createListener(ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        return new ImapSession(this, i4folder, handler);
    }

    @Override
    public List<String> getFlagList(boolean permanentOnly) {
        return flags.listNames(permanentOnly);
    }

    @Override
    public ImapFlag getTagByName(String tag) throws ServiceException {
        return new ImapFlag(mailbox.getTagByName(null, tag));
    }

    @Override
    public void resetImapUid(List<Integer> renumber) throws ServiceException {
        mailbox.resetImapUid(null, renumber);
    }

    @Override
    public Set<String> listSubscriptions(OperationContext octxt) throws ServiceException {
        return AccountUtil.parseConfig(getConfig(octxt, AccountUtil.SN_IMAP));
    }

    @Override
    public void saveSubscriptions(OperationContext octxt, Set<String> subs) throws ServiceException {
        MetadataList slist = new MetadataList();
        if (subs != null && !subs.isEmpty()) {
            for (String sub : subs)
                slist.add(sub);
        }
        mailbox.setConfig(octxt, AccountUtil.SN_IMAP, new Metadata().put(AccountUtil.FN_SUBSCRIPTIONS, slist));
    }

    private Metadata getConfig(OperationContext octxt, String section) throws ServiceException {
        return mailbox.getConfig(octxt, section);
    }

    @Override
    public void beginTrackingImap() throws ServiceException {
        mailbox.beginTrackingImap();
    }

    @Override
    public void deleteMessages(OperationContext octxt, List<Integer> ids) {
        for (int id : ids) {
            try {
                mailbox.delete(octxt, id, MailItem.Type.MESSAGE);
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("failed to delete message: %s", id);
            }
        }
    }

    @Override
    public List<ZimbraMailItem> imapCopy(OperationContext octxt, int[] itemIds, MailItemType type, int folderId)
            throws IOException, ServiceException {
        List<MailItem> mis = mailbox.imapCopy(octxt, itemIds, MailItem.Type.fromCommon(type), folderId);
        if (null == mis) {
            return Collections.emptyList();
        }
        List<ZimbraMailItem> zmis = Lists.newArrayListWithCapacity(mis.size());
        zmis.addAll(mis);
        return zmis;
    }

    @Override
    public InputStreamWithSize getByImapId(OperationContext octxt, int imapId, String folderId, String resolvedPath)
    throws ServiceException {
        MailItem mitem = mailbox.getItemByImapId(octxt, imapId, Integer.parseInt(folderId));
        if ((null == mitem) || (!ImapMessage.SUPPORTED_TYPES.contains(mitem.getType()))) {
            return null;
        }
        return ImapMessage.getContent(mitem);
    }

    @Override
    public void checkAppendMessageFlags(OperationContext octxt, List<AppendMessage> appends) throws ServiceException {
        mailbox.lock.lock();
        try {
            // TODO - Code taken with modifications from ImapHandler.doAPPEND
            // TODO - any reason why we can't use this.flags for flagset?
            ImapFlagCache flagset = ImapFlagCache.getSystemFlags();
            ImapFlagCache tagset = new ImapFlagCache( mailbox, octxt);
            for (AppendMessage append : appends) {
                append.checkFlags(flagset, tagset);
            }
        } finally {
            mailbox.lock.release();
        }
    }

    @Override
    public int getCurrentMODSEQ(int folderId) throws ServiceException {
        return mailbox.getFolderById(null, folderId).getImapMODSEQ();
    }

    @Override
    public Collection<FolderStore> getVisibleFolders(OperationContext octxt, ImapCredentials credentials,
            String owner, ImapPath relativeTo)
    throws ServiceException {
        Collection<Folder> folders = mailbox.getVisibleFolders(octxt);
        if (folders == null) {
            folders = mailbox.getFolderById(octxt, relativeTo == null ?
                    Mailbox.ID_FOLDER_USER_ROOT : relativeTo.asItemId().getId()).getSubfolderHierarchy();
        }
        String root = relativeTo == null ? "" : "/" + relativeTo.asResolvedPath();
        Collection<FolderStore> fStores = Sets.newHashSetWithExpectedSize(folders.size());
        for (Folder folder : folders) {
            if (!folder.getPath().startsWith(root) || folder.getPath().equals(root)) {
                continue;
            }
            fStores.add(folder);
        }
        return fStores;
    }

    @Override
    public List<ImapListener> getListeners(int folderId) {
        List<Session> sessions = mailbox.getListeners(Session.Type.IMAP);
        List<ImapListener> listeners = Lists.newArrayListWithCapacity(sessions.size());
        for (Session sess : sessions) {
            if (sess instanceof ImapSession) {
                ImapSession imapSess = (ImapSession) sess;
                if (folderId == imapSess.getFolderId()) {
                    listeners.add((ImapSession)sess);
                }
            }
        }
        return listeners;
    }

    public boolean attachmentsIndexingEnabled() throws ServiceException {
        return mailbox.attachmentsIndexingEnabled();
    }

    @Override
    public boolean addressMatchesAccountOrSendAs(String givenAddress) throws ServiceException {
        return (AccountUtil.addressMatchesAccountOrSendAs(mailbox.getAccount(), givenAddress));
    }

    @Override
    public MailboxStore getMailboxStore() {
        return mailbox;
    }

    /** Returns the ID of this mailbox's Account. */
    @Override
    public String getAccountId() {
        return mailbox.getAccountId();
    }

    @Override
    public Account getAccount() throws ServiceException {
        return mailbox.getAccount();
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    @Override
    public int getImapRECENTCutoff(FolderStore folder) {
        return ((Folder)folder).getImapRECENTCutoff();
    }

    @Override
    public int getImapRECENT(OperationContext ctxt, FolderStore folder) throws ServiceException {
        return mailbox.getImapRecent(ctxt, folder.getFolderIdInOwnerMailbox());
    }

    @Override
    public List<ImapMessage> openImapFolder(OperationContext octxt, ItemIdentifier folderId) throws ServiceException {
        return mailbox.openImapFolder(octxt, folderId.id);
    }

    @Override
    public void registerWithImapServerListener(ImapListener listener) {
        // Do nothing - use mailbox NOT ImapServerListener to monitor changes
    }

    @Override
    public void unregisterWithImapServerListener(ImapListener listener) {
        // Do nothing - use mailbox NOT ImapServerListener to monitor changes
    }
}
