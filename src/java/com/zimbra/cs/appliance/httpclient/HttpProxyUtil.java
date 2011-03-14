/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Appliance
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
package com.zimbra.cs.appliance.httpclient;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;


public class HttpProxyUtil {
    
    private static String sProxyUrl = null;
    private static URI sProxyUri = null;
    private static AuthScope sProxyAuthScope = null;
    private static UsernamePasswordCredentials sProxyCreds = null;
    
    public static synchronized void configureProxy(HttpClient client, String url) {
        try {
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
            client.getHostConfiguration().setProxy(sProxyUri.getHost(), sProxyUri.getPort());
            if (sProxyAuthScope != null && sProxyCreds != null) 
                client.getState().setProxyCredentials(sProxyAuthScope, sProxyCreds);
        } catch (URISyntaxException e) {
		throw new RuntimeException("Unable to configureProxy: ", e);
        }
    }
}
