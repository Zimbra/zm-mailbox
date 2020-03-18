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
package com.zimbra.common.mailbox;

import java.util.List;

public interface FolderStore extends BaseFolderInfo {
    /** Returns the folder's name.  Note that this is the folder's name (e.g. <code>"foo"</code>),
     * not its absolute pathname (e.g. <code>"/baz/bar/foo"</code>) for which, @see #getPath()
     */
    public MailboxStore getMailboxStore();
    public String getName();
    public ItemIdentifier getFolderItemIdentifier();
    public String getFolderIdAsString();
    public boolean isHidden();
    public boolean isDeletable();
    public boolean hasSubfolders();
    public boolean isInboxFolder();
    public boolean isSearchFolder();
    public boolean isContactsFolder();
    public boolean isChatsFolder();
    public boolean isSyncFolder();
    public boolean isIMAPSubscribed();
    public boolean inTrash();
    public boolean isVisibleInImap(boolean displayMailFoldersOnly);
    public List<ACLGrant> getACLGrants();
    public long getUIDValidity();
    /** @return number of items in folder, including IMAP \Deleted item */
    public int getImapMessageCount();
    /** @return number of unread items in folder, including IMAP \Deleted items */
    public int getImapUnreadCount();
    /** Returns a counter that increments each time an item is added to the folder. */
    public int getImapUIDNEXT();
    /** Returns the change number of the last time
     *  (a) an item was inserted into the folder or
     *  (b) an item in the folder had its flags or tags changed.
     *  This data is used to enable IMAP client synchronization via the CONDSTORE extension. */
    public int getImapMODSEQ();
}
