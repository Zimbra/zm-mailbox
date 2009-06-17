/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.httpclient;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;

public class HttpProxyUtil {
    
    private static String sProxyUrl = null;
    private static URI sProxyUri = null;
    private static AuthScope sProxyAuthScope = null;
    private static UsernamePasswordCredentials sProxyCreds = null;
    
    public static synchronized void configureProxy(HttpClient client) {
        try {
            String url = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraHttpProxyURL, null);
            if (url == null) return;

            // need to initializae all the statics
            if (sProxyUrl == null || !sProxyUrl.equals(url)) {
                sProxyUrl = url;
                sProxyUri = new URI(url);
                sProxyAuthScope = null;
                sProxyCreds = null;
                String userInfo = sProxyUri.getUserInfo();                
                if (userInfo != null) {
                    int i = userInfo.indexOf(':');
                    if (i != -1) {
                        sProxyAuthScope = new AuthScope(sProxyUri.getHost(), sProxyUri.getPort(), null); 
                        sProxyCreds = new UsernamePasswordCredentials(userInfo.substring(0, i), userInfo.substring(i+1));
                    }
                }
            }
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("setting proxy: "+url);
            }
            client.getHostConfiguration().setProxy(sProxyUri.getHost(), sProxyUri.getPort());
            if (sProxyAuthScope != null && sProxyCreds != null) 
                client.getState().setProxyCredentials(sProxyAuthScope, sProxyCreds);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Unable to configureProxy: "+e.getMessage(), e);
        } catch (URISyntaxException e) {
            ZimbraLog.misc.warn("Unable to configureProxy: "+e.getMessage(), e);
        }
    }
}
