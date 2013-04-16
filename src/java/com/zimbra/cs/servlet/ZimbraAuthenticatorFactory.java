/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 VMware, Inc.
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

package com.zimbra.cs.servlet;

import javax.servlet.ServletContext;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Authenticator.AuthConfiguration;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Server;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheMode;

/**
 * Jetty Authenticator Factory which adds support for 'ZimbraAuth' mechanism using Zimbra auth tokens
 *
 */
public class ZimbraAuthenticatorFactory extends DefaultAuthenticatorFactory {

    public static String ZIMBRA_AUTH_MECHANISM = "ZimbraAuth";

    private ZimbraAuthenticator zimbraAuthenticator = new ZimbraAuthenticator();

    public void setUrlPattern(String pattern) {
        zimbraAuthenticator.setUrlPattern(pattern);
    }

    @Override
    public Authenticator getAuthenticator(Server server, ServletContext context, AuthConfiguration configuration,
                    IdentityService identityService, LoginService loginService) {
        String auth = configuration.getAuthMethod();
        if (ZIMBRA_AUTH_MECHANISM.equalsIgnoreCase(auth)) {
            //force lazy load of non-caching prov
            //this is necessary when running in /zimbra webapp
            //if this class is ever used in /service webapp the caching prov _should_ already be loaded before we get here
            Provisioning.getInstance(CacheMode.OFF);
            return zimbraAuthenticator;
        } else {
            return super.getAuthenticator(server, context, configuration, identityService, loginService);
        }
    }
}
