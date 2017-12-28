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

public enum MailItemType {
        UNKNOWN,
        FOLDER, /** Item is a standard Folder. */
        SEARCHFOLDER, /** Item is a saved search - SearchFolder. */
        TAG, /** Item is a user-created Tag. */
        CONVERSATION, /** Item is a real, persisted Conversation. */
        MESSAGE, /** Item is a mail Message. */
        CONTACT, /** Item is a Contact. */
        @Deprecated INVITE, /** Item is a InviteMessage with a {@code text/calendar} MIME part. */
        DOCUMENT, /** Item is a bare Document. */
        NOTE, /** Item is a Note. */
        FLAG, /** Item is a memory-only system Flag. */
        APPOINTMENT, /** Item is a calendar Appointment. */
        VIRTUAL_CONVERSATION, /** Item is a memory-only, 1-message VirtualConversation. */
        MOUNTPOINT, /** Item is a Mountpoint pointing to a Folder, possibly in another user's Mailbox. */
        @Deprecated WIKI, /** Item is a WikiItem */
        TASK, /** Item is a Task */
        CHAT, /** Item is a Chat */
        COMMENT, /** Item is a Comment */
        LINK, /** Item is a Link pointing to a Document */
        SMARTFOLDER; /** Item a smart folder */
}