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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;


public class ImapFolder {
    private int mMailboxId;
    private int mItemId;
    private String mDataSourceId;
    private String mLocalPath;
    private String mRemotePath;
    private Long mUidValidity;
    
    public ImapFolder(int mailboxId, int id, String dataSourceId, String localPath, String remotePath, Long uidValidity) {
        mMailboxId = mailboxId;
        mItemId = id;
        mDataSourceId = dataSourceId;
        mLocalPath = localPath;
        mRemotePath = remotePath;
        mUidValidity = uidValidity;
    }
    
    public int getMailboxId() { return mMailboxId; }
    public int getItemId() { return mItemId; }
    public String getDataSourceId() { return mDataSourceId; }
    public String getLocalPath() { return mLocalPath; }
    public String getRemotePath() { return mRemotePath; }
    public Long getUidValidity() { return mUidValidity; }
    
    void setLocalPath(String localPath) { mLocalPath = localPath; }
    void setUidValidity(Long uidValidity) { mUidValidity = uidValidity; }
    
    public String toString() {
        return String.format("ImapFolder: { mailboxId=%d, itemId=%d, dataSourceId=%s, localPath=%s, remotePath=%s, uidValidity=%d }",
            mMailboxId, mItemId, mDataSourceId, mLocalPath, mRemotePath, mUidValidity);
    }
}
