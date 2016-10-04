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

import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.session.Session;

public interface ImapMailboxStore extends MailboxStore {
    public static ImapMailboxStore get(Mailbox mbox) {
        if (mbox == null) {
            return null;
        }
        return new LocalImapMailboxStore(mbox);
    }

    public static ImapMailboxStore get(MailboxStore mailboxStore, String accountId) {
        if (mailboxStore instanceof Mailbox) {
            return new LocalImapMailboxStore((Mailbox) mailboxStore);
        }
        if (mailboxStore instanceof ZMailbox) {
            return new RemoteImapMailboxStore((ZMailbox) mailboxStore, accountId);
        }
        return null;
    }

    public ImapFlag getFlagByName(String name);
    public List<String> getFlagList(boolean permanentOnly);
    public ImapFlag getTagByName(String tag) throws ServiceException;
    public void resetImapUid(List<Integer> renumber) throws ServiceException;
    public Set<ImapMessage> getSubsequence(ImapFolder i4folder, String tag, String sequenceSet, boolean byUID)
            throws ImapParseException;
    public void setConfig(OperationContext octxt, String section, Metadata config) throws ServiceException;
    public Metadata getConfig(OperationContext octxt, String section) throws ServiceException;
    public void beginTrackingImap() throws ServiceException;
    public void deleteMessages(OperationContext octxt, List<Integer> ids);
    public List<MailItem> imapCopy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
            throws IOException, ServiceException;
    public void checkAppendMessageFlags(OperationContext octxt, List<AppendMessage> appends) throws ServiceException;
    public int getCurrentMODSEQ(int folderId) throws ServiceException;
    public List<Session> getListeners();
    public boolean attachmentsIndexingEnabled() throws ServiceException;
    public boolean addressMatchesAccountOrSendAs(String givenAddress) throws ServiceException;
    public int getId();
    /** Returns this mailbox's Account. */
    Account getAccount() throws ServiceException;
    /** Returns the ID of this mailbox's Account. */
    public String getAccountId();
    public Collection<FolderStore> getVisibleFolders(OperationContext octxt, ImapCredentials credentials,
            String owner, ImapPath relativeTo)
    throws ServiceException;
}
