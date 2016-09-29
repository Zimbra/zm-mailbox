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
import java.util.List;
import java.util.Set;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.session.Session;

public class RemoteImapMailboxStore implements ImapMailboxStore {

    private transient ZMailbox mailbox;

    public RemoteImapMailboxStore(ZMailbox mailbox) throws ServiceException {
        this.mailbox = mailbox;
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
                    mailbox.deleteMessage(String.valueOf(id));
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
    public List<Session> getListeners() {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    @Override
    public int getId() {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }

    /** Returns the ID of this mailbox's Account. */
    @Override
    public String getAccountId() {
        throw new UnsupportedOperationException("RemoteImapMailboxStore method not supported yet");
    }
}
