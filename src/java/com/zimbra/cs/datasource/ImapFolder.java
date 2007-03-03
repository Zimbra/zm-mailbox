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


public class ImapFolder {
    private int mMailboxId;
    private int mId;
    private String mDataSourceId;
    private String mFolderPath;
    
    public ImapFolder(int mailboxId, int id, String dataSourceId, String folderPath) {
        mMailboxId = mailboxId;
        mId = id;
        mDataSourceId = dataSourceId;
        mFolderPath = folderPath;
    }
    
    public int getMailboxId() { return mMailboxId; }
    public int getId() { return mId; }
    public String getDataSourceId() { return mDataSourceId; }
    public String getFolderPath() { return mFolderPath; }
    
    public String toString() {
        return String.format("ImapFolder: { mailboxId=%d, id=%d, dataSourceId=%s, folderPath=%s }",
            mMailboxId, mId, mDataSourceId, mFolderPath);
    }
}
