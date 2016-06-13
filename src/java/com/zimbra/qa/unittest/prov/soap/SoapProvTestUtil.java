/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
