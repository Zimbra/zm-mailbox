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

import com.zimbra.common.mailbox.MailboxStore;

public interface ImapListener {
    public String getAuthenticatedAccountId();
    public String getTargetAccountId();
    public MailboxStore getMailbox();
    public ImapFolder getImapFolder() throws ImapSessionClosedException;
    public ImapPath getPath();
    public boolean hasNotifications();
    public void updateAccessTime();
    public void closeFolder(boolean isUnregistering);
    public boolean isWritable();
    public int getFolderId();
    public void incrementRenumber(ImapMessage msg);
    public boolean isFailedRenumber(ImapMessage msg);
}
