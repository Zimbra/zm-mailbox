/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.util.yauth;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;

public class AuthTest extends TestCase {
    private static final String APPID = "D2hTUBHAkY0IEL5MA7ibTS_1K86E8RErSSaTGn4-";
    private static final String USER = "dacztest";
    private static final String PASS = "test1234";
    
    private static String token;

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }
    
    private static String getToken() throws Exception {
        if (token == null) {
            token = RawAuth.getToken(APPID, USER, PASS);
        }
        return token;
    }
    
    public void testToken() throws Exception {
        token = getToken();
        assertNotNull(token);
    }

    public void testAuthenticate() throws Exception {
        RawAuth auth = RawAuth.authenticate(APPID, getToken());
        assertNotNull(auth.getWSSID());
        assertNotNull(auth.getCookie());
    }

    public void testInvalidPassword() throws Exception {
        Exception error = null;
        try {
            RawAuth.getToken(APPID, USER, "invalid");
        } catch (AuthenticationException e) {
            error = e;
        }
        assertNotNull(error);
    }
}
