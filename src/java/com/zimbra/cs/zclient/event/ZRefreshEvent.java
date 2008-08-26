/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
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

package com.zimbra.cs.zclient.event;

import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZSoapSB;
import com.zimbra.cs.zclient.ZTag;

import java.util.List;

public class ZRefreshEvent {

    private long mSize;
    private ZFolder mUserRoot;
    private List<ZTag> mTags;

    public ZRefreshEvent(long size, ZFolder userRoot, List<ZTag> tags) {
    	mSize = size;
    	mUserRoot = userRoot;
    	mTags = tags;
    }

    /**
     * @return size of mailbox in bytes
     */
    public long getSize() {
        return mSize;
    }

    /**
     * return the root user folder
     * @return user root folder
     */
    public ZFolder getUserRoot() {
        return mUserRoot;
    }

    public List<ZTag> getTags() {
        return mTags;
    }
    
    public String toString() {
    	ZSoapSB sb = new ZSoapSB();
    	sb.beginStruct();
    	sb.add("size", getSize());
    	sb.add("userRoot", mUserRoot.toString());
    	sb.add("tags", mTags, false, false);
    	sb.endStruct();
    	return sb.toString();
    }
}
