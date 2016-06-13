/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.script;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.util.BuildInfo;

public class ScriptUtil {

    /**
     * Initializes provisioning with the default admin settings.
     */
    public static void initProvisioning()
    throws ServiceException {
        initProvisioning(null);
    }
    
    /**
     * Initializes the default {@link Provisioning} configuration,
     * based on values that were passed to the methods in this class.
     * By default, connects to <tt>https://localhost:7071/service/admin/soap</tt>
     * with <tt>zimbra_ldap_user</tt> and <tt>zimbra_ldap_password</tt>.
     * 
     * @param options provisioning options, or <tt>null</tt> for default options
     */
    public static void initProvisioning(ProvisioningOptions options)
    throws ServiceException {
        if (options == null) {
            options = new ProvisioningOptions();
        }
        CliUtil.toolSetup();
        SoapProvisioning sp = new SoapProvisioning();

        String userAgent = options.getUserAgent();
        String userAgentVersion = options.getUserAgentVersion();
        if (userAgent == null) {
            userAgent = "Zimbra Scripting";
            userAgentVersion = BuildInfo.VERSION;
        }
        SoapTransport.setDefaultUserAgent(userAgent, userAgentVersion);
        
        String uri = options.getSoapURI();
        if (uri == null) {
            uri = LC.zimbra_admin_service_scheme.value() + "localhost:7071" + AdminConstants.ADMIN_SERVICE_URI;
        }
        sp.soapSetURI(uri);
        
        String user = options.getUsername();
        if (user == null) {
            user = LC.zimbra_ldap_user.value();
        }
        
        String password = options.getPassword();
        if (password == null) {
            password = LC.zimbra_ldap_password.value();
        }
        sp.soapAdminAuthenticate(user, password);
        
        Provisioning.setInstance(sp);
    }
}
