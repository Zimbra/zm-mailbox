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

package com.zimbra.cs.zclient.event;

import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZSoapSB;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.common.service.ServiceException;

import java.util.List;
import java.util.ArrayList;

public class ZRefreshEvent {

    private long mSize = 0;
    private ZFolder mUserRoot = null;
    private List<ZTag> mTags = new ArrayList<ZTag>();

    public ZRefreshEvent(long size, ZFolder userRoot, List<ZTag> tags) throws ServiceException {
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
