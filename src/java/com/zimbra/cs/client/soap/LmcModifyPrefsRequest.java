/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.DomUtil;


public class LmcModifyPrefsRequest extends LmcSoapRequest {

    private Map mPrefMods;      

    public void setPrefMods(Map m) { mPrefMods = m; }
    
    public Map getPrefMods() { return mPrefMods; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountService.MODIFY_PREFS_REQUEST);

        Set s = mPrefMods.entrySet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            Element pe = DomUtil.add(request, AccountService.E_PREF, 
                                     (String) entry.getValue());  
            DomUtil.addAttr(pe, AccountService.A_NAME, (String) entry.getKey());
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
    {
        // there is no data provided in the response
        return new LmcModifyPrefsResponse();
    }

}
