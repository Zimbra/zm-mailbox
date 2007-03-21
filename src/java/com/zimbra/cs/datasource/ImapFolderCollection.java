/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImapFolderCollection
implements Iterable<ImapFolder>
{
    private Map<String, ImapFolder> mByLocalPath = new HashMap<String, ImapFolder>();
    private Map<String, ImapFolder> mByRemotePath = new HashMap<String, ImapFolder>();
    private Map<Integer, ImapFolder> mByItemId = new HashMap<Integer, ImapFolder>();
    
    public void add(ImapFolder imapFolder) {
        mByLocalPath.put(imapFolder.getLocalPath(), imapFolder);
        mByRemotePath.put(imapFolder.getRemotePath(), imapFolder);
        mByItemId.put(imapFolder.getItemId(), imapFolder);
    }
    
    public void remove(ImapFolder imapFolder) {
        mByLocalPath.remove(imapFolder.getLocalPath());
        mByRemotePath.remove(imapFolder.getRemotePath());
        mByItemId.remove(imapFolder.getItemId());
    }
    
    public ImapFolder getByLocalPath(String localPath) {
        return mByLocalPath.get(localPath);
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
