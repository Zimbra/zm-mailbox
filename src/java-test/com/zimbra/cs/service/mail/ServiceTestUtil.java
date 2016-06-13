/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.MockHttpServletRequest;
import com.zimbra.cs.service.MockHttpServletResponse;
import com.zimbra.soap.DocumentService;
import com.zimbra.soap.MockSoapEngine;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class ServiceTestUtil {

    public static Map<String, Object> getRequestContext(Account acct) throws Exception {
        return getRequestContext(acct, acct);
    }

    public static Map<String, Object> getExternalRequestContext(String externalEmail, Account targetAcct) throws Exception {
        return getRequestContext(new GuestAccount(externalEmail, "password"), targetAcct);
    }

    public static Map<String, Object> getRequestContext(Account authAcct, Account targetAcct) throws Exception {
        return getRequestContext(authAcct, targetAcct, new MailService());
    }

    public static Map<String, Object> getRequestContext(Account authAcct, Account targetAcct, DocumentService service) throws Exception {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(authAcct), targetAcct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));
        context.put(SoapServlet.SERVLET_REQUEST, new MockHttpServletRequest("test".getBytes("UTF-8"), new URL("http://localhost:7070/service/FooRequest"), ""));
        context.put(SoapEngine.ZIMBRA_ENGINE, new MockSoapEngine(service));
        context.put(SoapServlet.SERVLET_RESPONSE, new MockHttpServletResponse());
        return context;
    }

}
