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
