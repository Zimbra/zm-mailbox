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

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.Element;

import java.util.HashMap;
import java.util.Map;

public class ZIdentity  {

    private String mName;
    private Map<String, String> mAttrs;

    public ZIdentity(Element e) throws ServiceException {
        mName = e.getAttribute(AccountService.A_NAME);
        mAttrs = new HashMap<String, String>();
        for (Element a : e.listElements(AccountService.E_A)) {
            mAttrs.put(a.getAttribute(AccountService.A_NAME), a.getText());
        }
    }

    public ZIdentity(String name, Map<String, String> attrs) {
        mName = name;
        mAttrs = attrs;
    }

    public String getName() {
        return mName;
    }

    public Map<String, Object> getAttrs() {
        return new HashMap<String, Object>(mAttrs);
    }

    public boolean getBool(String name) {
        return Provisioning.TRUE.equals(mAttrs.get(name));
    }

    public String getBccAddress() { return mAttrs.get(Provisioning.A_zimbraPrefBccAddress); }

    public String getForwardIncludeOriginalText() { return mAttrs.get(Provisioning.A_zimbraPrefForwardIncludeOriginalText); }

    public boolean getForwardIncludeAsAttachment() { return "includeAsAttachment".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBody() { return "includeBody".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getForwardIncludeOriginalText()); }

    public String getForwardReplyFormat() { return mAttrs.get(Provisioning.A_zimbraPrefForwardReplyFormat); }
    public boolean getForwardReplyTextFormat() { return "text".equals(getForwardReplyFormat()); }
    public boolean getForwardReplyHtmlFormat() { return "html".equals(getForwardReplyFormat()); }
    public boolean getForwardReplySameFormat() { return "same".equals(getForwardReplyFormat()); }

    public String getForwardReplyPrefixChar() { return mAttrs.get(Provisioning.A_zimbraPrefForwardReplyPrefixChar); }

    public String getFromAddress() { return mAttrs.get(Provisioning.A_zimbraPrefFromAddress); }

    public String getFromDisplay() { return mAttrs.get(Provisioning.A_zimbraPrefFromDisplay); }

    public String getSignature() { return mAttrs.get(Provisioning.A_zimbraPrefMailSignature); }

    public boolean getSignatureEnabled() { return getBool(Provisioning.A_zimbraPrefMailSignatureEnabled); }

    public String getSignatureStyle() { return mAttrs.get(Provisioning.A_zimbraPrefMailSignatureStyle); }
    public boolean getSignatureStyleTop() { return "outlook".equals(getSignatureStyle()); }
    public boolean getSignatureStyleBottom() { return "internet".equals(getSignatureStyle()); }

    public String getReplyIncludeOriginalText() { return mAttrs.get(Provisioning.A_zimbraPrefReplyIncludeOriginalText); }

    public boolean getReplyIncludeAsAttachment() { return "includeAsAttachment".equals(getForwardIncludeOriginalText()); }
    public boolean getReplyIncludeBody() { return "includeBody".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeNone() { return "includeNone".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeSmart() { return "includeSmart".equals(getReplyIncludeOriginalText()); }


    public String getReplyToAddress() { return mAttrs.get(Provisioning.A_zimbraPrefReplyToAddress); }

    public String getReplyToDisplay() { return mAttrs.get(Provisioning.A_zimbraPrefReplyToDisplay); }

    public boolean getReplyToEnabled() { return getBool(Provisioning.A_zimbraPrefReplyToEnabled); }

    public boolean getSaveToSent() { return getBool(Provisioning.A_zimbraPrefSaveToSent); }

    public String getSentMailFolder() { return mAttrs.get(Provisioning.A_zimbraPrefSentMailFolder); }

    public boolean getUseDefaultIdentitySettings() { return getBool(Provisioning.A_zimbraPrefUseDefaultIdentitySettings); }

    public String getWhenInFolderIds() { return mAttrs.get(Provisioning.A_zimbraPrefWhenInFolderIds); }

    public boolean getWhenInFoldersEnabled() { return getBool(Provisioning.A_zimbraPrefWhenInFoldersEnabled); }

    public String getWhenSentToAddresses() { return mAttrs.get(Provisioning.A_zimbraPrefWhenSentToAddresses); }

    public boolean getWhenSentToEnabled() { return getBool(Provisioning.A_zimbraPrefWhenSentToEnabled); }

    public Element toElement(Element parent) {
        Element identity = parent.addElement(AccountService.E_IDENTITY);
        identity.addAttribute(AccountService.A_NAME, mName);
        for (Map.Entry<String,String> entry : mAttrs.entrySet()) {
            Element a = identity.addElement(AccountService.E_A);
            a.addAttribute(AccountService.A_NAME, entry.getKey());
            a.setText(entry.getValue());
        }
        return identity;
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.beginStruct("attrs");
        for (Map.Entry<String, String> entry : mAttrs.entrySet()) {
            sb.add(entry.getKey(), entry.getValue());
        }
        sb.endStruct();
        sb.endStruct();
        return sb.toString();
    }

}
