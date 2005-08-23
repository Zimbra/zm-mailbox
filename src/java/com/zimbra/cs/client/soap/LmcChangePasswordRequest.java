/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.service.ServiceException;

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
        Element request = DocumentHelper.createElement(AccountService.CHANGE_PASSWORD_REQUEST);
        // <account>
        Element a = DomUtil.add(request, AccountService.E_ACCOUNT, mAccount);
        DomUtil.addAttr(a, AdminService.A_BY, BY_NAME);
        // <old password>
        DomUtil.add(request, AccountService.E_OLD_PASSWORD, mOldPassword);  
        // <password>
        DomUtil.add(request, AccountService.E_PASSWORD, mPassword);  
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
