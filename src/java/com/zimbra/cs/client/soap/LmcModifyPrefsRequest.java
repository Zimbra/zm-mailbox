/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
