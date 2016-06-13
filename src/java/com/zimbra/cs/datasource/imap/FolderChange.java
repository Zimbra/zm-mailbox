/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.mailbox.Folder;

class FolderChange {
    public enum Type {
        ADDED, MOVED, DELETED
    }

    private final Type type;
    private final int itemId;
    private final Folder folder;
    private final ImapFolder tracker;

    public static FolderChange added(Folder folder) {
        return new FolderChange(Type.ADDED, folder.getId(), folder, null);
    }

    public static FolderChange moved(Folder folder, ImapFolder tracker) {
        return new FolderChange(Type.MOVED, folder.getId(), folder, tracker);
    }

    public static FolderChange deleted(int itemId, ImapFolder tracker) {
        return new FolderChange(Type.DELETED, itemId, null, tracker);
    }
    
    private FolderChange(Type type, int itemId, Folder folder, ImapFolder tracker) {
        this.type = type;
        this.itemId = itemId;
        this.folder = folder;
        this.tracker = tracker;
    }

    public Type getType() {
        return type;
    }

    public int getItemId() {
        return itemId;
    }

    public Folder getFolder() {
        return folder;
    }

    public ImapFolder getTracker() {
        return tracker;
    }

    public boolean isAdded() {
        return type == Type.ADDED;
    }

    public boolean isMoved() {
        return type == Type.MOVED;
    }

    public boolean isDeleted() {
        return type == Type.DELETED;
    }
}
