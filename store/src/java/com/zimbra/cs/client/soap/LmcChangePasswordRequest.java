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

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;

public class LmcChangePasswordRequest extends LmcSoapRequest {

    private String mOldPassword;
    private String mPassword;
    private String mAccount;

    private static final String BY_NAME = "name";

    public void setOldPassword(String o) { mOldPassword = o; }
    public void setPassword(String p) { mPassword = p; }
    public void setAccount(String a) { mAccount = a; }

    public String getOldPassword() { return mOldPassword; }
    public String getPassword() { return mPassword; }
    public String getAccount() { return mAccount; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountConstants.CHANGE_PASSWORD_REQUEST);
        // <account>
        Element a = DomUtil.add(request, AccountConstants.E_ACCOUNT, mAccount);
        DomUtil.addAttr(a, AdminConstants.A_BY, BY_NAME);
        // <old password>
        DomUtil.add(request, AccountConstants.E_OLD_PASSWORD, mOldPassword);
        // <password>
        DomUtil.add(request, AccountConstants.E_PASSWORD, mPassword);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException
    {
        // there is no response to the request, only a fault
        LmcChangePasswordResponse response = new LmcChangePasswordResponse();
        return response;
    }

}
