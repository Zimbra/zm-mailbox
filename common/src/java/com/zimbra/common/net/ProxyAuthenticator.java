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
package com.zimbra.common.net;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;

/**
 * Authenticator implementation for HTTP/SOCKS proxy username password
 */
public class ProxyAuthenticator extends Authenticator {
    
    private Map<Proxy.Type, UsernamePassword> userPasswords = new HashMap<Proxy.Type, UsernamePassword>();
    
    public ProxyAuthenticator() {
        super();
    }
    
    public void addCredentials(Proxy.Type type, UsernamePassword userPass) {
        userPasswords.put(type, userPass);
    }
    
    private UsernamePassword getUsernamePassword() {
        String reqProto = getRequestingProtocol().toLowerCase();
        UsernamePassword uPass = null;
        if (reqProto.startsWith("http")) {
            uPass = userPasswords.get(Proxy.Type.HTTP);
        } else if (reqProto.startsWith("sock")) {
            uPass = userPasswords.get(Proxy.Type.SOCKS);
        } 
        if (uPass == null) {
            throw new RuntimeException();
        } else {
            return uPass;
        }
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (getRequestorType() == RequestorType.PROXY) {
            UsernamePassword uPass = getUsernamePassword();
            return new PasswordAuthentication(uPass.getUsername(), uPass.getPassword().toCharArray());
        } else {
            ZimbraLog.net.warn("Non-proxy authentication type %s requested, unable to fulfil", getRequestorType());
            return null;
        }
    }
    
}
