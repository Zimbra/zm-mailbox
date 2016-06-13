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

package com.zimbra.client;

import java.util.Collection;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.Identity;

public class ZIdentity  implements ToZJSONObject {

    private Identity data;

    public ZIdentity(Identity data) {
        this.data = data;
        
    }

    public ZIdentity(String name, Map<String, Object> attrs) {
        data = Identity.fromName(name);
        data.setAttrs(Attr.fromMultimap(StringUtil.toNewMultimap(attrs)));
        data.setId(data.getFirstMatchingAttr(ZAttrProvisioning.A_zimbraPrefIdentityId));
    }
    
    public Identity getData() {
        return new Identity(data);
    }

    public String getRawName() {
        return data.getName();
    }

    public String getName() {
        return get(ZAttrProvisioning.A_zimbraPrefIdentityName);
    }

    public String getId() {
        return data.getId();
    }
    
    /**
     * @param name name of pref to get
     * @return null if unset, or first value in list
     */
    public String get(String name) {
        return data.getFirstMatchingAttr(name);
    }
    
    public Map<String, Object> getAttrs() {
        return StringUtil.toOldMultimap(data.getAttrsMultimap());
    }

    public boolean getBool(String name) {
        return ProvisioningConstants.TRUE.equals(get(name));
    }

    public boolean getIsDefault() { return ProvisioningConstants.DEFAULT_IDENTITY_NAME.equals(getRawName()); }
    
    public String getFromAddress() { return get(ZAttrProvisioning.A_zimbraPrefFromAddress); }

    public String getFromDisplay() { return get(ZAttrProvisioning.A_zimbraPrefFromDisplay); }

    public ZEmailAddress getFromEmailAddress() {
        return new ZEmailAddress(getFromAddress(), null, getFromDisplay(), ZEmailAddress.EMAIL_TYPE_FROM);
    }

    public String getSignatureId() { return get(ZAttrProvisioning.A_zimbraPrefDefaultSignatureId); }

    public String getReplyToAddress() { return get(ZAttrProvisioning.A_zimbraPrefReplyToAddress); }

    public String getReplyToDisplay() { return get(ZAttrProvisioning.A_zimbraPrefReplyToDisplay); }

    public ZEmailAddress getReplyToEmailAddress() {
        return new ZEmailAddress(getReplyToAddress(), null, getReplyToDisplay(), ZEmailAddress.EMAIL_TYPE_REPLY_TO);
    }

    public boolean getReplyToEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefReplyToEnabled); }

    public String[] getMulti(String name) {
        Collection<String> values = data.getAttrsMultimap().get(name);
        String[] valArray = new String[values.size()];
        values.toArray(valArray);
        return valArray;
    }
    
    public String[] getWhenInFolderIds() {
        return getMulti(ZAttrProvisioning.A_zimbraPrefWhenInFolderIds);
    }

    public boolean getWhenInFoldersEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefWhenInFoldersEnabled); }

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
        identity.addAttribute(AccountConstants.A_NAME, getRawName()).addAttribute(AccountConstants.A_ID, getId());
        for (Map.Entry<String,String> entry : data.getAttrsMultimap().entries()) {
            identity.addKeyValuePair(entry.getKey(), entry.getValue(), AccountConstants.E_A,  AccountConstants.A_NAME);
        }
        return identity;
    }

    @Override
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

    @Override
    public String toString() {
        return String.format("[ZIdentity %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

}
