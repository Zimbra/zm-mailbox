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

import com.zimbra.common.service.ServiceException;

public interface FolderStore {
    /** Returns the folder's absolute path.  Paths are UNIX-style with <code>'/'</code> as the path delimiter.
     * Paths are relative to *  the user root folder, which has the path <code>"/"</code>.  So the Inbox's path is
     *  <code>"/Inbox"</code>, etc.
     */
    public String getPath();
    /** Returns the folder's name.  Note that this is the folder's name (e.g. <code>"foo"</code>),
     * not its absolute pathname (e.g. <code>"/baz/bar/foo"</code>) for which, @see #getPath()
     */
    public MailboxStore getMailboxStore();
    public String getName();
    public String getFolderIdAsString();
    public int getFolderIdInOwnerMailbox();
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
    public int getUIDValidity();
    /** @return number of items in folder, including IMAP \Deleted item */
    public int getImapMessageCount();
    /** @return number of unread items in folder, including IMAP \Deleted items */
    public int getImapUnreadCount();
    /**
     * Returns the number of messages in the folder that would be considered \Recent in an IMAP session.
     * If there is currently a READ-WRITE IMAP session open on the folder, by definition all other IMAP connections
     *  will see no \Recent messages.  <i>(Note that as such, this method should <u>not</u> be called by IMAP sessions
     *  that have this folder selected.)</i>  Otherwise, it is the number of messages/chats/contacts added to the
     *  folder, moved to the folder, or edited in the folder since the last such IMAP session.
     */
    public int getImapRECENT() throws ServiceException;
    /** Returns a counter that increments each time an item is added to the folder. */
    public int getImapUIDNEXT();
    /** Returns the change number of the last time
     *  (a) an item was inserted into the folder or
     *  (b) an item in the folder had its flags or tags changed.
     *  This data is used to enable IMAP client synchronization via the CONDSTORE extension. */
    public int getImapMODSEQ();
}
