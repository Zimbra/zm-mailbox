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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.HashMap;
import java.util.Iterator;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.ServiceException;


public class LmcGetPrefsRequest extends LmcSoapRequest {

    private String mPrefsToGet[];
    

    /**
     * Set the preferences to retrieve.
     * @param prefsToGet[] - array of names of prefs to get.  Pass in null 
     * for all preferences
     */
    public void setPrefsToGet(String prefsToGet[]) { mPrefsToGet = prefsToGet; }
    
    public String[] getPrefsToGet() { return mPrefsToGet; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountService.GET_PREFS_REQUEST);
        if (mPrefsToGet != null) {
            for (int i = 0; i < mPrefsToGet.length; i++) {
                    Element pe = DomUtil.add(request, AccountService.E_PREF, "");  
                    DomUtil.addAttr(pe, AccountService.A_NAME, mPrefsToGet[i]);  
            }
        }
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        // iterate over all the <pref> elements in the response
        HashMap prefMap = new HashMap();
        for (Iterator ait = responseXML.elementIterator(AccountService.E_PREF); ait.hasNext(); ) {
            Element a = (Element) ait.next();
            addPrefToMultiMap(prefMap, a);
        }

        // create the response object and put in the HashMap
        LmcGetPrefsResponse response = new LmcGetPrefsResponse();
        response.setPrefsMap(prefMap);
        return response;
    }

}
