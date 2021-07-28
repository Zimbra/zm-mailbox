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

public final class FolderConstants {
    private FolderConstants() {
    }

    public static final int ID_AUTO_INCREMENT = -1;
    public static final int ID_FOLDER_USER_ROOT = 1;
    public static final int ID_FOLDER_INBOX = 2;
    public static final int ID_FOLDER_TRASH = 3;
    public static final int ID_FOLDER_SPAM = 4;
    public static final int ID_FOLDER_SENT = 5;
    public static final int ID_FOLDER_DRAFTS = 6;
    public static final int ID_FOLDER_CONTACTS = 7;
    public static final int ID_FOLDER_TAGS = 8;
    public static final int ID_FOLDER_CONVERSATIONS = 9;
    public static final int ID_FOLDER_CALENDAR = 10;
    public static final int ID_FOLDER_ROOT = 11;

    // no longer created in new mailboxes since Helix (bug 39647).  old mailboxes may still contain a system folder with id 12
    @Deprecated
    public static final int ID_FOLDER_NOTEBOOK  = 12;
    public static final int ID_FOLDER_AUTO_CONTACTS = 13;
    public static final int ID_FOLDER_IM_LOGS = 14;
    public static final int ID_FOLDER_TASKS = 15;
    public static final int ID_FOLDER_BRIEFCASE = 16;
    public static final int ID_FOLDER_COMMENTS  = 17;
    // ID_FOLDER_PROFILE Was used for folder related to ProfileServlet which was used in pre-release Iron Maiden only.
    // Old mailboxes may still contain a system folder with id 18
    @Deprecated
    public static final int ID_FOLDER_PROFILE = 18;

    public static final int ID_FOLDER_UNSUBSCRIBE = 19;

    public static final int HIGHEST_SYSTEM_ID = 19;
}
