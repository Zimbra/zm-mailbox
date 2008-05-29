/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;


public class ImapFolder {
    private final int mMailboxId;
    private final int mItemId;
    private final String mDataSourceId;
    private String mLocalPath;
    private String mRemotePath;
    private Long mUidValidity;
    private long mUidNext = -1; // Used to optimize offline data source
    
    public ImapFolder(int mailboxId, int id, String dataSourceId,
                      String localPath, String remotePath, Long uidValidity) {
        mMailboxId = mailboxId;
        mItemId = id;
        mDataSourceId = dataSourceId;
        mLocalPath = localPath;
        mRemotePath = remotePath;
        mUidValidity = uidValidity;
    }

    public ImapFolder(ImapFolder folder) {
        mMailboxId = folder.mMailboxId;
        mItemId = folder.mItemId;
        mDataSourceId = folder.mDataSourceId;
        mLocalPath = folder.mLocalPath;
        mRemotePath = folder.mRemotePath;
        mUidValidity = folder.mUidValidity;
        mUidNext = folder.mUidNext;
    }
    
    public int getMailboxId() { return mMailboxId; }
    public int getItemId() { return mItemId; }
    public String getDataSourceId() { return mDataSourceId; }
    public String getLocalPath() { return mLocalPath; }
    public String getRemotePath() { return mRemotePath; }
    public Long getUidValidity() { return mUidValidity; }
    public long getUidNext() { return mUidNext; }
    
    public void setLocalPath(String localPath) { mLocalPath = localPath; }
    public void setRemotePath(String remotePath) { mRemotePath = remotePath; }
    public void setUidValidity(Long uidValidity) { mUidValidity = uidValidity; }
    public void setUidNext(long uidNext) { mUidNext = uidNext; }

    public String toString() {
        return String.format("ImapFolder: { mailboxId=%d, itemId=%d, dataSourceId=%s, localPath=%s, remotePath=%s, uidValidity=%d }",
            mMailboxId, mItemId, mDataSourceId, mLocalPath, mRemotePath, mUidValidity);
    }
}
