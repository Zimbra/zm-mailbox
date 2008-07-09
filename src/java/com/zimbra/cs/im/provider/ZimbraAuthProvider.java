/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
 
package com.zimbra.cs.im.provider;

import org.jivesoftware.wildfire.auth.AuthProvider;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.user.UserNotFoundException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class ZimbraAuthProvider implements AuthProvider {

    public ZimbraAuthProvider() {
        
    }
    
    public String getServerDialbackHmac(String data) throws Exception {
        return ServerDialbackKey.getHmac(data);
    }

    public void authenticate(String username, String password) throws UnauthorizedException {
        try {
            Account acct = ZimbraUserProvider.getInstance().lookupAccount(username);
            if (acct == null) {
                throw new UnauthorizedException("Unknown user: "+username);
            }
            Provisioning.getInstance().authAccount(acct, password, "im"); 
        } catch (ServiceException e) {
            throw new UnauthorizedException(e);
        }
    }

    public void authenticate(String username, String token, String digest) throws UnauthorizedException {
        throw new UnsupportedOperationException("Digest authentication not currently supported.");
    }

    public boolean isDigestSupported() {
        return false;
    }

    public boolean isPlainSupported() {
        return true;
    }

    public String getPassword(String username) throws UserNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setPassword(String username, String password) throws UserNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsPasswordRetrieval() {
        return false;
    }

}
