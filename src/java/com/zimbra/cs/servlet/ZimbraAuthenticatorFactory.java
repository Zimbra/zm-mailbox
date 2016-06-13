/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.servlet;

import javax.servlet.ServletContext;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Authenticator.AuthConfiguration;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Server;

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
            return zimbraAuthenticator;
        } else {
            return super.getAuthenticator(server, context, configuration, identityService, loginService);
        }
    }
}
