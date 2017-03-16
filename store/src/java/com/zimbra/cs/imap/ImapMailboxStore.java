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
import com.zimbra.common.util.InputStreamWithSize;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;

public abstract class ImapMailboxStore {

    protected transient ImapFlagCache flags;

    protected ImapMailboxStore() {
        this.flags = ImapFlagCache.getSystemFlags();
    }

    public static ImapMailboxStore get(MailboxStore mbox) throws ServiceException {
        if (mbox instanceof Mailbox) {
            return new LocalImapMailboxStore((Mailbox) mbox);
        }
        if (mbox instanceof ZMailbox) {
            return new RemoteImapMailboxStore((ZMailbox) mbox);
        }
        return null;
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

    public ImapFlag getFlagByName(String name) {
        return flags.getByImapName(name);
    }

    public List<String> getFlagList(boolean permanentOnly) {
        return flags.listNames(permanentOnly);
    }

    public abstract ImapFlag getTagByName(String tag) throws ServiceException;
    public abstract void resetImapUid(List<Integer> renumber) throws ServiceException;
    public abstract void beginTrackingImap() throws ServiceException;
    public abstract void deleteMessages(OperationContext octxt, List<Integer> ids);
    public abstract List<MailItem> imapCopy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
            throws IOException, ServiceException;
    public abstract InputStreamWithSize getByImapId(OperationContext octxt, int imapId, String folderId, String resolvedPath)
            throws ServiceException;
    public abstract void checkAppendMessageFlags(OperationContext octxt, List<AppendMessage> appends) throws ServiceException;
    public abstract int getCurrentMODSEQ(int folderId) throws ServiceException;
    public abstract List<ImapListener> getListeners();
    public abstract boolean addressMatchesAccountOrSendAs(String givenAddress) throws ServiceException;
    public abstract int getId();
    public abstract MailboxStore getMailboxStore();
    /** Returns this mailbox's Account. */
    public abstract Account getAccount() throws ServiceException;
    /** Returns the ID of this mailbox's Account. */
    public abstract String getAccountId();
    public abstract Collection<FolderStore> getVisibleFolders(OperationContext octxt, ImapCredentials credentials,
            String owner, ImapPath relativeTo)
    throws ServiceException;
    public abstract Set<String> listSubscriptions(OperationContext octxt) throws ServiceException;
    public abstract void saveSubscriptions(OperationContext octxt, Set<String> subs) throws ServiceException;
}
