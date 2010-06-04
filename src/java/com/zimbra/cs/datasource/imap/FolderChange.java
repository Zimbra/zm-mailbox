/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
