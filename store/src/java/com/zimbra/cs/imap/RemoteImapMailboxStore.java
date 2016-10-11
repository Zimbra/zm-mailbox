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
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.store.Blob;

public class RemoteImapMailboxStore implements ImapMailboxStore {

    private transient ZMailbox zMailbox;
    private transient String accountId;

    public RemoteImapMailboxStore(ZMailbox mailbox, String accountId) {
        this.zMailbox = mailbox;
        this.accountId = accountId;
    }

    @Override
    public ImapFlag getFlagByName(String name) {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public List<String> getFlagList(boolean permanentOnly) {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public ImapFlag getTagByName(String tag) throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public void resetImapUid(List<Integer> renumber) throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public Set<ImapMessage> getSubsequence(ImapFolder i4folder, String tag, String sequenceSet, boolean byUID)
    throws ImapParseException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public void setConfig(OperationContext octxt, String section, Metadata config) throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public Metadata getConfig(OperationContext octxt, String section) throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public void beginTrackingImap() throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
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

    @Override
    public List<MailItem> imapCopy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
            throws IOException, ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public void checkAppendMessageFlags(OperationContext octxt, List<AppendMessage> appends) throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public int getCurrentMODSEQ(int folderId) throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public Collection<FolderStore> getVisibleFolders(OperationContext octxt, ImapCredentials credentials,
            String owner, ImapPath relativeTo)
    throws ServiceException {
        String root = relativeTo == null ? "" : "/" + relativeTo.asResolvedPath();
        List<ZFolder> allZFolders = zMailbox.getAllFolders();
        Set<FolderStore> fStores = Sets.newHashSetWithExpectedSize(allZFolders.size());
        for (ZFolder zfolder : zMailbox.getAllFolders()) {
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
    public List<Session> getListeners() {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public boolean attachmentsIndexingEnabled() throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public boolean addressMatchesAccountOrSendAs(String givenAddress) throws ServiceException {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    public int store(String folderId, Blob content, Date date, int msgFlags)
    throws ImapSessionClosedException, ServiceException, IOException {
        String id;
        try (InputStream is = content.getInputStream()) {
            id = zMailbox.addMessage(folderId, Flag.toString(msgFlags), null, date.getTime(), is,
                    content.getRawSize(), true);
        }
        return new ItemId(id, accountId).getId();
    }

    @Override
    public int getId() {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
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
}
