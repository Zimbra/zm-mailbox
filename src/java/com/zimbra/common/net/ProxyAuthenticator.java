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
