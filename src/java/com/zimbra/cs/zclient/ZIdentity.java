/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.Identity;

public class ZIdentity  implements ToZJSONObject {

    private Identity data;

    public ZIdentity(Identity data) {
        this.data = data;
        
    }

    public ZIdentity(String name, Map<String, Object> attrs) {
        data = new Identity();
        data.setName(name);
        data.setAttrs(Attr.fromMultimap(StringUtil.toNewMultimap(attrs)));
        data.setId(get(Provisioning.A_zimbraPrefIdentityId));
    }
    
    public Identity getData() {
        return new Identity(data);
    }

    public String getRawName() {
        return data.getName();
    }

    public String getName() {
        return get(Provisioning.A_zimbraPrefIdentityName);
    }

    public String getId() {
        return data.getId();
    }
    
    /**
     * @param name name of pref to get
     * @return null if unset, or first value in list
     */
    public String get(String name) {
        Collection<String> values = data.getAttrsMultimap().get(name);
        Iterator<String> iter = values.iterator();
        if (!iter.hasNext()) {
            return null;
        }
        return iter.next();
    }
    
    public Map<String, Object> getAttrs() {
        return StringUtil.toOldMultimap(data.getAttrsMultimap());
    }

    public boolean getBool(String name) {
        return Provisioning.TRUE.equals(get(name));
    }

    public boolean isDefault() { return Provisioning.DEFAULT_IDENTITY_NAME.equals(getName()); }
    
    public String getFromAddress() { return get(Provisioning.A_zimbraPrefFromAddress); }

    public String getFromDisplay() { return get(Provisioning.A_zimbraPrefFromDisplay); }

    public ZEmailAddress getFromEmailAddress() {
        return new ZEmailAddress(getFromAddress(), null, getFromDisplay(), ZEmailAddress.EMAIL_TYPE_FROM);
    }

    public String getSignatureId() { return get(Provisioning.A_zimbraPrefDefaultSignatureId); }

    public String getReplyToAddress() { return get(Provisioning.A_zimbraPrefReplyToAddress); }

    public String getReplyToDisplay() { return get(Provisioning.A_zimbraPrefReplyToDisplay); }

    public ZEmailAddress getReplyToEmailAddress() {
        return new ZEmailAddress(getReplyToAddress(), null, getReplyToDisplay(), ZEmailAddress.EMAIL_TYPE_REPLY_TO);
    }

    public boolean getReplyToEnabled() { return getBool(Provisioning.A_zimbraPrefReplyToEnabled); }

    public String[] getMulti(String name) {
        Collection<String> values = data.getAttrsMultimap().get(name);
        String[] valArray = new String[values.size()];
        values.toArray(valArray);
        return valArray;
    }
    
    public String[] getWhenInFolderIds() {
        return getMulti(Provisioning.A_zimbraPrefWhenInFolderIds);
    }

    public boolean getWhenInFoldersEnabled() { return getBool(Provisioning.A_zimbraPrefWhenInFoldersEnabled); }

    public boolean containsFolderId(String folderId) {
        for (String id : getWhenInFolderIds()) {
            if (id.equals(folderId)) {
                return true;
            }
        }
        return false;
    }

    public Element toElement(Element parent) {
        Element identity = parent.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, getName()).addAttribute(AccountConstants.A_ID, getId());
        for (Map.Entry<String,String> entry : data.getAttrsMultimap().entries()) {
            identity.addKeyValuePair(entry.getKey(), entry.getValue(), AccountConstants.E_A,  AccountConstants.A_NAME);
        }
        return identity;
    }
    
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("name", getName());
        zjo.put("id", getId());
        JSONObject jo = new JSONObject();
        zjo.put("attrs",jo);
        for (Map.Entry<String, String> entry : data.getAttrsMultimap().entries()) {
            jo.put(entry.getKey(), entry.getValue());
        }
        return zjo;
    }

    public String toString() {
        return String.format("[ZIdentity %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

}
