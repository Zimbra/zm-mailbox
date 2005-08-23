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

/*
 * Created on 2005. 4. 27.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.httpclient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class URLUtil {

    private static Log mLog = LogFactory.getLog(URLUtil.class);

    private static final String SCHEME_HTTP  = "http://";
    private static final String SCHEME_HTTPS = "https://";

    /**
     * Returns absolute URL with scheme, host, and port for mail app on server.
     * If server supports both SSL and non-SSL, non-SSL takes precedence.
     * @param server
     * @param path what follows port number; begins with slash
     * @return
     */
    public static String getMailURL(Server server, String path) {
        String scheme = SCHEME_HTTP;
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        int port = 0;

        int httpPort = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
        if (httpPort > 0) {
        	port = httpPort;
            scheme = SCHEME_HTTP;
        } else {
            int httpsPort = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
            if (httpsPort > 0) {
            	port = httpsPort;
                scheme = SCHEME_HTTPS;
            } else {
                // Bad configuration...  Just assume HTTP on port 80.
                scheme = SCHEME_HTTP;
                port = 80;
            }
        }

        StringBuffer sb = new StringBuffer(128);
        sb.append(scheme).append(hostname).append(":").append(port).append(path);
        return sb.toString();
    }

    /**
     * Returns absolute URL with scheme, host, and port for admin app on server.
     * Admin app only runs over SSL.
     * @param server
     * @param path what follows port number; begins with slash
     * @return
     */
    public static String getAdminURL(Server server, String path) {
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        int port = server.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        StringBuffer sb = new StringBuffer(128);
        sb.append(SCHEME_HTTPS).append(hostname).append(":").append(port).append(path);
        return sb.toString();
    }
}
