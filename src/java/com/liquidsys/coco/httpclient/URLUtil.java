/*
 * Created on 2005. 4. 27.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.httpclient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;

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
        String hostname = server.getAttr(Provisioning.A_liquidServiceHostname);
        int port = 0;

        int httpPort = server.getIntAttr(Provisioning.A_liquidMailPort, 0);
        if (httpPort > 0) {
        	port = httpPort;
            scheme = SCHEME_HTTP;
        } else {
            int httpsPort = server.getIntAttr(Provisioning.A_liquidMailSSLPort, 0);
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
        String hostname = server.getAttr(Provisioning.A_liquidServiceHostname);
        int port = server.getIntAttr(Provisioning.A_liquidAdminPort, 0);
        StringBuffer sb = new StringBuffer(128);
        sb.append(SCHEME_HTTPS).append(hostname).append(":").append(port).append(path);
        return sb.toString();
    }
}
