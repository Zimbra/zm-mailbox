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
    public boolean isHidden();
    public boolean isSearchFolder();
    public boolean isContactsFolder();
    public boolean isChatsFolder();
    public boolean isFlaggedAsSyncFolder();
    public boolean inTrash();
    public boolean isVisibleInImap(boolean displayMailFoldersOnly);
    public int getUIDValidity();
}
