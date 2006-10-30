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
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZIdentity  {

    private String mName;
    private List<Signature> mSignatures;
    private Map<String, String> mAttrs;

    public ZIdentity(Element e) throws ServiceException {
        mName = e.getAttribute(MailService.A_NAME);
        mSignatures = new ArrayList<Signature>();
        for (Element s : e.listElements(MailService.E_SIGNATURE)) {
            mSignatures.add(new Signature(s));
        }
        mAttrs = new HashMap<String, String>();
        for (Element a : e.listElements(MailService.E_ATTRIBUTE)) {
            mAttrs.put(a.getAttribute(MailService.A_ATTRIBUTE_NAME), a.getText());
        }
    }

    public ZIdentity(String name, Map<String, String> attrs, List<Signature> signatures) {
         mName = name;
        mAttrs = attrs;
        mSignatures = signatures;
    }

    public String getName() {
        return mName;
    }

    public List<Signature> getSignatures() {
        return mSignatures;
    }

    public Element toElement(Element parent) {
        Element identity = parent.addElement(AccountService.E_IDENTITY);
        identity.addAttribute(MailService.A_NAME, mName);
        for (Map.Entry<String,String> entry : mAttrs.entrySet()) {
            Element a = identity.addElement(MailService.E_ATTRIBUTE);
            a.addAttribute(MailService.A_ATTRIBUTE_NAME, entry.getKey());
            a.setText(entry.getValue());
        }
        for (Signature s : mSignatures) {
            s.toElement(identity);
        }
        return identity;
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.add("signatures", mSignatures, false, true);
        sb.beginStruct("attrs");
        for (Map.Entry<String, String> entry : mAttrs.entrySet()) {
            sb.add(entry.getKey(), entry.getValue());
        }
        sb.endStruct();
        sb.endStruct();
        return sb.toString();
    }


    public static class Signature {
        private String mName;
        private String mValue;

        public Signature(Element e) throws ServiceException {
            mName = e.getAttribute(AccountService.A_NAME);
            mValue = e.getText();
        }

        public Signature(String name, String value) {
            mName = name;
            mValue = value;
        }

        public Element toElement(Element parent) {
            Element signature = parent.addElement(AccountService.E_SIGNATURE);
            signature.addAttribute(AccountService.A_NAME, mName);
            signature.setText(mValue);
            return signature;
        }
        
        public String getName() { return mName; }
        public String getValue() { return mValue; }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add("name", mName);
            sb.add("value", mValue);
            sb.endStruct();
            return sb;
        }
    }


}
