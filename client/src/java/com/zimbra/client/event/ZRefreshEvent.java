/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client.event;

import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZJSONObject;
import com.zimbra.client.ZSmartFolder;
import com.zimbra.client.ZTag;

import org.json.JSONException;

import java.util.List;

public class ZRefreshEvent implements ToZJSONObject {

    private long mSize;
    private ZFolder mUserRoot;
    private List<ZTag> mTags;
    private List<ZSmartFolder> mSmartFolders;

    public ZRefreshEvent(long size, ZFolder userRoot, List<ZTag> tags) {
        this(size, userRoot, tags, null);
    }

    public ZRefreshEvent(long size, ZFolder userRoot, List<ZTag> tags, List<ZSmartFolder> smartFolders) {
    	mSize = size;
    	mUserRoot = userRoot;
    	mTags = tags;
    	mSmartFolders = smartFolders;
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

    public List<ZSmartFolder> getSmartFolders() {
        return mSmartFolders;
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("size", getSize());
    	zjo.put("userRoot", mUserRoot);
    	zjo.put("tags", mTags);
    	zjo.put("smartFolders", mSmartFolders);
    	return zjo;
    }

    @Override
    public String toString() {
        return "[ZRefreshEvent]";
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
