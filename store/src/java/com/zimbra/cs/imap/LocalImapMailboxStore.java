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
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;

public class LocalImapMailboxStore implements ImapMailboxStore {

    private transient Mailbox mailbox;
    private transient ImapFlagCache flags;

    public LocalImapMailboxStore(Mailbox mailbox) {
        this.mailbox = mailbox;
        try {
            this.flags = ImapFlagCache.getSystemFlags(mailbox);
        } catch (ServiceException se) {
            // Realistically shouldn't happen
            ZimbraLog.imap.debug("Problem getting system flags for mailbox %s - using empty set", mailbox, se);
            this.flags = new ImapFlagCache();
        }
    }

    @Override
    public ImapFlag getFlagByName(String name) {
        return flags.getByImapName(name);
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
    public Set<ImapMessage> getSubsequence(ImapFolder i4folder, String tag, String sequenceSet, boolean byUID)
    throws ImapParseException {
        Set<ImapMessage> i4set;
        mailbox.lock.lock();
        try {
            i4set = i4folder.getSubsequence(tag, sequenceSet, byUID);
        } finally {
            mailbox.lock.release();
        }
        return i4set;
    }

    @Override
    public void setConfig(OperationContext octxt, String section, Metadata config) throws ServiceException {
        mailbox.setConfig(octxt, section, config);
    }

    @Override
    public Metadata getConfig(OperationContext octxt, String section) throws ServiceException {
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
    public List<MailItem> imapCopy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
            throws IOException, ServiceException {
        return  mailbox.imapCopy(octxt, itemIds, type, folderId);

    }

    @Override
    public void checkAppendMessageFlags(OperationContext octxt, List<AppendMessage> appends) throws ServiceException {
        mailbox.lock.lock();
        try {
            // TODO - Code taken with modifications from ImapHandler.doAPPEND
            // TODO - any reason why we can't use this.flags for flagset?
            ImapFlagCache flagset = ImapFlagCache.getSystemFlags(mailbox);
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
    public List<Session> getListeners() {
        return mailbox.getListeners(Session.Type.IMAP);
    }

    @Override
    public boolean attachmentsIndexingEnabled() throws ServiceException {
        return mailbox.attachmentsIndexingEnabled();
    }

    @Override
    public boolean addressMatchesAccountOrSendAs(String givenAddress) throws ServiceException {
        return (AccountUtil.addressMatchesAccountOrSendAs(mailbox.getAccount(), givenAddress));
    }

    @Override
    public int getId() {
        return mailbox.getId();
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
}
