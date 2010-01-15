/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.soap.AccountConstants;


public class LmcModifyPrefsRequest extends LmcSoapRequest {

    private Map mPrefMods;

    public void setPrefMods(Map m) { mPrefMods = m; }

    public Map getPrefMods() { return mPrefMods; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountConstants.MODIFY_PREFS_REQUEST);

        Set s = mPrefMods.entrySet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            Element pe = DomUtil.add(request, AccountConstants.E_PREF,
                                     (String) entry.getValue());
            DomUtil.addAttr(pe, AccountConstants.A_NAME, (String) entry.getKey());
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
    {
        // there is no data provided in the response
        return new LmcModifyPrefsResponse();
    }

}
