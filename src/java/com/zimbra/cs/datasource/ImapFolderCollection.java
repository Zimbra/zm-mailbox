/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.datasource;

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

    public ImapFolderCollection(ImapFolderCollection folders) {
        mByItemId = new HashMap<Integer, ImapFolder>(folders.mByItemId);
        mByLocalPath = new HashMap<String, ImapFolder>(folders.mByLocalPath);
        mByRemotePath = new HashMap<String, ImapFolder>(folders.mByRemotePath);
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

    public Iterator<String> getRemotePathsIterator() {
        return mByRemotePath.keySet().iterator();
    }
}
