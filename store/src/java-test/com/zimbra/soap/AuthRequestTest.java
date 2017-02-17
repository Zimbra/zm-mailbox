/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap;

import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.type.PreAuth;
import com.zimbra.soap.type.AccountSelector;

import org.junit.Test;
import static org.junit.Assert.*;


import com.zimbra.common.soap.Element;


public class AuthRequestTest {

    private final String username = "someUsername";

    private final String password = "somePass";

    private final long expires = 1600000;

    private final long timestamp = expires + 60000;

    @Test
    public void testBuildAuthRequestWithPassword()
    {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setAccount(AccountSelector.fromName(username));
        authRequest.setPassword(password);

        try {
            Element element = JaxbUtil.jaxbToElement(authRequest);
            String xml = element.toString();
            assertTrue(element.hasChildren());
            Element account = element.getElement("account");
            Element pwdE = element.getElement("password");
            assertEquals("Username embedded in request is incorrect", username, account.getText());
            assertEquals("Password embedded in request is incorrect", password, pwdE.getText());
        } catch (ServiceException e) {
            fail("Encountered an exception: " + e);
        }
    }

    @Test
    public void testBuildAuthRequestWithPreAuth()
    {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setAccount(AccountSelector.fromName(username));
        PreAuth preAuth = new PreAuth()
            .setExpires(expires)
            .setTimestamp(timestamp);
        authRequest.setPreauth(preAuth);

        try {
            Element element = JaxbUtil.jaxbToElement(authRequest);
            String xml = element.toString();

            Element account = element.getElement("account");
            assertEquals("Username embedded in request is incorrect", username, account.getText());
            Element preauth = element.getElement("preauth");
            assertEquals("'expires' embedded in preauth is incorrect", Long.toString(expires), preauth.getAttribute("expires"));
            assertEquals("'timestamp' embedded in preauth is incorrect", Long.toString(timestamp), preauth.getAttribute("timestamp"));

        } catch (ServiceException e) {
            fail("Encountered a problem: " + e);
        }
    }

}
