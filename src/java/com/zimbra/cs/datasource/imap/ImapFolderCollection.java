/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.datasource.imap.ImapFolder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImapFolderCollection implements Iterable<ImapFolder> {
    private final Map<Integer, ImapFolder> mByItemId;
    private final Map<String, ImapFolder> mByLocalPath;
    private final Map<String, ImapFolder> mByRemotePath;

    public ImapFolderCollection() {
        mByItemId = new HashMap<Integer, ImapFolder>();
        mByLocalPath = new HashMap<String, ImapFolder>();
        mByRemotePath = new HashMap<String, ImapFolder>();
    }

    public void add(ImapFolder imapFolder) {
        mByLocalPath.put(imapFolder.getLocalPath().toLowerCase(), imapFolder);
        mByRemotePath.put(imapFolder.getRemoteId(), imapFolder);
        mByItemId.put(imapFolder.getItemId(), imapFolder);
    }
    
    public void remove(ImapFolder imapFolder) {
        mByLocalPath.remove(imapFolder.getLocalPath().toLowerCase());
        mByRemotePath.remove(imapFolder.getRemoteId());
        mByItemId.remove(imapFolder.getItemId());
    }
    
    public ImapFolder getByLocalPath(String localPath) {
        return mByLocalPath.get(localPath.toLowerCase());
    }
    
    public ImapFolder getByRemotePath(String remotePath) {
        return mByRemotePath.get(remotePath);
    }
    
    public ImapFolder getByItemId(int itemId) {
        return mByItemId.get(itemId);
    }
    
    public int size() {
        return mByItemId.size();
    }

    public Iterator<ImapFolder> iterator() {
        return mByItemId.values().iterator();
    }
}
