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

import com.zimbra.cs.zclient.ToZJSONObject;
import com.zimbra.cs.zclient.ZJSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ZDeleteEvent implements ToZJSONObject {

    private String mIds;
    private List<String> mList;

    public ZDeleteEvent(String ids) {
        mIds = ids;
    }

    public String getIds() {
        return mIds;
    }

    public synchronized List<String> toList() {
        if (mList == null) {
            mList = new ArrayList<String>();
            if (mIds != null && mIds.length() > 0)
            	for (String id : mIds.split(","))
            		mList.add(id);
        }
        return mList;
    }
    
    public String toString() {
        return String.format("[ZDeleteEvent %s]", getIds());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.putList("ids", mList);
        return zjo;
    }
}
