/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util.yauth;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import junit.framework.TestCase;


public class AuthTest extends TestCase {
    private static final String APPID = "D2hTUBHAkY0IEL5MA7ibTS_1K86E8RErSSaTGn4-";
    private static final String USER = "dacztest";
    private static final String PASS = "test1234";
    
    private static String token;

    static {
        Configurator.reconfigure();
        Configurator.setRootLevel(Level.DEBUG);
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
