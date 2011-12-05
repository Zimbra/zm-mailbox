/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.qa.unittest.prov.ProvTestUtil;

public class SoapProvTestUtil extends ProvTestUtil {
    
    SoapProvTestUtil() throws Exception {
        super(SoapProvisioning.getAdminInstance());
        SoapProvisioning prov = getProv();
        prov.soapSetHttpTransportDebugListener(new SoapDebugListener());
    }
    
    SoapProvisioning getProv() {
        return (SoapProvisioning) prov;
    }
    
    static SoapProvisioning getSoapProvisioning(String userName, String password) 
    throws ServiceException {
        SoapProvisioning sp = new SoapProvisioning();
        sp.soapSetHttpTransportDebugListener(new SoapDebugListener());
        sp.soapSetURI("https://localhost:7071" + AdminConstants.ADMIN_SERVICE_URI);
        sp.soapAdminAuthenticate(userName, password);
        return sp;
    }
}
