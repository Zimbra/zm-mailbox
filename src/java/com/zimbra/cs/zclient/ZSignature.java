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
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class ZSignature implements Comparable, ToZJSONObject {

    private String mName;
    private String mId;
    private String mValue;

    public ZSignature(Element e) throws ServiceException {
        mName = e.getAttribute(AccountConstants.A_NAME);
        mId = e.getAttribute(AccountConstants.A_ID, null);
        
        for (Element c : e.listElements(AccountConstants.E_CONTENT)) {
            String type = c.getAttribute(AccountConstants.A_TYPE, null);
            if (type.equals("text/plain"))
                mValue = c.getText();
        }
    }

    public ZSignature(String id, String name, String value) {
        mId = id;
        mName = name;
        mValue = value;
    }

    public ZSignature(String name, String value) {
        mName = name;
        mValue = value;
    }

    public String getName() {
        return mName;
    }

    public String getId() {
        return mId;
    }

    public String getValue() { return mValue; }
    
    public Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraSignatureId, mId);
        attrs.put(Provisioning.A_zimbraSignatureName, mName);
        attrs.put(Provisioning.A_zimbraPrefMailSignature, mValue);
        return attrs;
    }
    
    public Element toElement(Element parent) {
        Element sig = parent.addElement(AccountConstants.E_SIGNATURE);
        sig.addAttribute(AccountConstants.A_NAME, mName);
        if (mId != null) sig.addAttribute(AccountConstants.A_ID, mId);
        if (mName != null) sig.addAttribute(AccountConstants.A_NAME, mName);
        if (mValue != null) {
            Element content = sig.addElement(AccountConstants.E_CONTENT);
            content.addAttribute(AccountConstants.A_TYPE, "text/plain");
            content.setText(mValue);
        }
        return sig;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("name", mName);
        zjo.put("id", mId);
        zjo.put("value", mValue);
        return zjo;
    }

    public String toString() {
        return ZJSONObject.toString(this);
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof ZSignature))
            return 0;
        ZSignature other = (ZSignature) obj;
        return getName().compareTo(other.getName());
    }
}
