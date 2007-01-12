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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZIdentity  {

    private String mName;
    private String mId;
    private Map<String, Object> mAttrs;

    public ZIdentity(Element e) throws ServiceException {
        mName = e.getAttribute(AccountConstants.A_NAME);
        mId = e.getAttribute(AccountConstants.A_ID, null);
        mAttrs = new HashMap<String, Object>();
        for (Element a : e.listElements(AccountConstants.E_A)) {
            StringUtil.addToMultiMap(mAttrs, a.getAttribute(AccountConstants.A_NAME), a.getText());
        }
    }

    public ZIdentity(String name, Map<String, Object> attrs) {
        mName = name;
        mAttrs = attrs;
        mId = get(Provisioning.A_zimbraPrefIdentityId);
    }

    public String getName() {
        return mName;
    }

    public String getId() {
        return mId;
    }
    
    /**
     * @param name name of pref to get
     * @return null if unset, or first value in list
     */
    public String get(String name) {
        Object value = mAttrs.get(name);
        if (value == null) {
            return null;
        } else if (value instanceof String[]) {
            return ((String[])value)[0];
        } else if (value instanceof List) {
            return (String) ((List)value).get(0);
        } else {
            return value.toString();
        }
    }
    
    public Map<String, Object> getAttrs() {
        return new HashMap<String, Object>(mAttrs);
    }

    public boolean getBool(String name) {
        return Provisioning.TRUE.equals(get(name));
    }

    public boolean isDefault() { return mName.equals(Provisioning.DEFAULT_IDENTITY_NAME); }
    
    public String getBccAddress() { return get(Provisioning.A_zimbraPrefBccAddress); }

    public String getForwardIncludeOriginalText() { return get(Provisioning.A_zimbraPrefForwardIncludeOriginalText); }

    public boolean getForwardIncludeAsAttachment() { return "includeAsAttachment".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBody() { return "includeBody".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getForwardIncludeOriginalText()); }

    public String getForwardReplyFormat() { return get(Provisioning.A_zimbraPrefForwardReplyFormat); }
    public boolean getForwardReplyTextFormat() { return "text".equals(getForwardReplyFormat()); }
    public boolean getForwardReplyHtmlFormat() { return "html".equals(getForwardReplyFormat()); }
    public boolean getForwardReplySameFormat() { return "same".equals(getForwardReplyFormat()); }

    public String getForwardReplyPrefixChar() { return get(Provisioning.A_zimbraPrefForwardReplyPrefixChar); }

    public String getFromAddress() { return get(Provisioning.A_zimbraPrefFromAddress); }

    public String getFromDisplay() { return get(Provisioning.A_zimbraPrefFromDisplay); }

    public ZEmailAddress getFromEmailAddress() {
        return new ZEmailAddress(getFromAddress(), null, getFromDisplay(), ZEmailAddress.EMAIL_TYPE_FROM);
    }

    public String getSignature() { return get(Provisioning.A_zimbraPrefMailSignature); }

    public boolean getSignatureEnabled() { return getBool(Provisioning.A_zimbraPrefMailSignatureEnabled); }

    public String getSignatureStyle() { return get(Provisioning.A_zimbraPrefMailSignatureStyle); }
    public boolean getSignatureStyleTop() { return "outlook".equals(getSignatureStyle()); }
    public boolean getSignatureStyleBottom() { return "internet".equals(getSignatureStyle()); }

    public String getReplyIncludeOriginalText() { return get(Provisioning.A_zimbraPrefReplyIncludeOriginalText); }

    public boolean getReplyIncludeAsAttachment() { return "includeAsAttachment".equals(getForwardIncludeOriginalText()); }
    public boolean getReplyIncludeBody() { return "includeBody".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeNone() { return "includeNone".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeSmart() { return "includeSmart".equals(getReplyIncludeOriginalText()); }


    public String getReplyToAddress() { return get(Provisioning.A_zimbraPrefReplyToAddress); }

    public String getReplyToDisplay() { return get(Provisioning.A_zimbraPrefReplyToDisplay); }

    public ZEmailAddress getReplyToEmailAddress() {
        return new ZEmailAddress(getReplyToAddress(), null, getReplyToDisplay(), ZEmailAddress.EMAIL_TYPE_REPLY_TO);
    }

    public boolean getReplyToEnabled() { return getBool(Provisioning.A_zimbraPrefReplyToEnabled); }

    public boolean getSaveToSent() { return getBool(Provisioning.A_zimbraPrefSaveToSent); }

    public String getSentMailFolder() { return get(Provisioning.A_zimbraPrefSentMailFolder); }

    public boolean getUseDefaultIdentitySettings() { return getBool(Provisioning.A_zimbraPrefUseDefaultIdentitySettings); }

    public String[] getMulti(String name) {
        Object o = mAttrs.get(name);
        if (o instanceof String[]) {
            return (String[]) o;
        } else if (o instanceof String) {
            return new String[] { o.toString() };
        } else {
            return new String[0];
        }
    }
    
    public String[] getWhenInFolderIds() {
        return getMulti(Provisioning.A_zimbraPrefWhenInFolderIds);
    }

    public boolean getWhenInFoldersEnabled() { return getBool(Provisioning.A_zimbraPrefWhenInFoldersEnabled); }

    public synchronized boolean containsAddress(ZEmailAddress address) {
        for (String addr: getWhenSentToAddresses()) {
            if (addr.toLowerCase().contains(address.getAddress().toLowerCase())) {
                    return true;
            }
        }
        return false;      
    }
    
    public boolean containsFolderId(String folderId) {
        for (String id : getWhenInFolderIds()) {
            if (id.equals(folderId)) {
                return true;
            }
        }
        return false;
    }

    public String[] getWhenSentToAddresses() { return getMulti(Provisioning.A_zimbraPrefWhenSentToAddresses); }

    public boolean getWhenSentToEnabled() { return getBool(Provisioning.A_zimbraPrefWhenSentToEnabled); }

    public Element toElement(Element parent) {
        Element identity = parent.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, mName).addAttribute(AccountConstants.A_ID, mId);
        for (Map.Entry<String,Object> entry : mAttrs.entrySet()) {
            if (entry.getValue() instanceof String[]) {
                String[] values = (String[]) entry.getValue();
                for (String value : values) {
                    Element a = identity.addElement(AccountConstants.E_A);
                    a.addAttribute(AccountConstants.A_NAME, entry.getKey());
                    a.setText(value);
                }
            } else {
                Element a = identity.addElement(AccountConstants.E_A);
                a.addAttribute(AccountConstants.A_NAME, entry.getKey());
                a.setText(entry.getValue().toString());
            }
        }
        return identity;
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.add("id", mId);        
        sb.beginStruct("attrs");
        for (Map.Entry<String, Object> entry : mAttrs.entrySet()) {
            if (entry.getValue() instanceof String[]) {
                String[] values = (String[]) entry.getValue();
                sb.add(entry.getKey(), values, false, true);
            } else {
                sb.add(entry.getKey(), entry.getValue().toString());
            }
        }
        sb.endStruct();
        sb.endStruct();
        return sb.toString();
    }

}
