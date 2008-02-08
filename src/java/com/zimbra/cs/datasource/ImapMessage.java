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

public class ImapMessage {

    private long mUid;
    private int mItemId;
    private int mFlags;
    private int mTrackedFlags;
    
    public ImapMessage(long uid, int itemId, int flags, int trackedFlags) {
        mUid = uid;
        mItemId = itemId;
        mFlags = flags;
        mTrackedFlags = trackedFlags;
    }
    
    public long getUid() { return mUid; }
    public int getItemId() { return mItemId; }
    public int getFlags() { return mFlags; }
    public int getTrackedFlags() { return mTrackedFlags; }
}
