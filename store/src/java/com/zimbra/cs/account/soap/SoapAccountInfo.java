/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.soap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.admin.message.GetAccountInfoResponse;
import com.zimbra.soap.admin.type.Attr;


public class SoapAccountInfo {
    
    private Map<String,Object> mAttrs;
    private String mName;
    private List<String> mSoapURL;
    private String mAdminSoapURL;
    
    SoapAccountInfo(GetAccountInfoResponse resp)
    throws ServiceException {
        mAttrs = Attr.collectionToMap(resp.getAttrList());
        mName = resp.getName();
        mSoapURL = resp.getSoapURLList();
        mAdminSoapURL = resp.getAdminSoapURL();
    }

     SoapAccountInfo(Element e) throws ServiceException {
        mAttrs = SoapProvisioning.getAttrs(e);
        mName = e.getElement(AdminConstants.E_NAME).getText();
        mSoapURL = new ArrayList<String>();
        for (Element su : e.listElements(AdminConstants.E_SOAP_URL)) {
            mSoapURL.add(su.getText());
        }
        mAdminSoapURL = e.getElement(AdminConstants.E_ADMIN_SOAP_URL).getText();
    }
    
    public List<String> getSoapURL() { return mSoapURL; }
    public String getAdminSoapURL() { return mAdminSoapURL; }
    
    public String getAttr(String name) {
        Object v = mAttrs.get(name);
        if (v instanceof String) {
            return (String) v;
        } else if (v instanceof String[]) {
            String[] a = (String[]) v;
            return a.length > 0 ? a[0] : null;
        } else {
            return null;
        }
    }

    public String getAttr(String name, String defaultValue) {
        String v = getAttr(name);
        return v == null ? defaultValue : v;
    }

}