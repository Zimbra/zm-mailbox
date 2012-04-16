/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.MockHttpServletRequest;
import com.zimbra.soap.MockSoapEngine;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class ServiceTestUtil {

    public static Map<String, Object> getRequestContext(Account acct) throws Exception {
        return ServiceTestUtil.getRequestContext(acct, acct);
    }

    public static Map<String, Object> getRequestContext(Account authAcct, Account targetAcct) throws Exception {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(authAcct), targetAcct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));
        context.put(SoapServlet.SERVLET_REQUEST, new MockHttpServletRequest("test".getBytes("UTF-8"), new URL("http://localhost:7070/service/FooRequest"), ""));
        context.put(SoapEngine.ZIMBRA_ENGINE, new MockSoapEngine(new MailService()));
        return context;
    }

}
