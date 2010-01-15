/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.DataSource.Type;


public class ZRssDataSource implements ZDataSource, ToZJSONObject {

    private String mId;
    private String mName;
    private String mFolderId;
    private boolean mEnabled;
    
    public ZRssDataSource(String name, String folderId, boolean enabled) {
        mName = name;
        mFolderId = folderId;
        mEnabled = enabled;
    }
    
    public ZRssDataSource(Element e)
    throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mName = e.getAttribute(MailConstants.A_NAME);
        mEnabled = e.getAttributeBool(MailConstants.A_DS_IS_ENABLED);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER);
    }
    
    public Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceId, mId);
        attrs.put(Provisioning.A_zimbraDataSourceName, mName);
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, mEnabled ? "TRUE" : "FALSE");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, mFolderId);
        return attrs;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public Type getType() {
        return Type.rss;
    }
    
    public String getFolderId() {
        return mFolderId;
    }
    
    public boolean isEnabled() {
        return mEnabled;
    }

    public Element toElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_RSS);
        if (mId != null) src.addAttribute(MailConstants.A_ID, mId);
        src.addAttribute(MailConstants.A_NAME, mName);
        src.addAttribute(MailConstants.A_DS_IS_ENABLED, mEnabled);
        src.addAttribute(MailConstants.A_FOLDER, mFolderId);
        return src;
    }

    public Element toIdElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS_RSS);
        src.addAttribute(MailConstants.A_ID, mId);
        return src;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("name", mName);
        zjo.put("enabled", mEnabled);
        zjo.put("folderId", mFolderId);
        return zjo;
    }

}
