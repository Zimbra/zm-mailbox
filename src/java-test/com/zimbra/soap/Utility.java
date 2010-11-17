/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.soap;

import java.util.ArrayList;
import java.util.List;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.developer.WSBindingProvider;
import com.sun.xml.bind.api.JAXBRIContext;

import com.zimbra.soap.header.HeaderContext;
import com.zimbra.soap.account.wsimport.AccountService_Service;
import com.zimbra.soap.account.wsimport.AccountService;
import com.zimbra.soap.account.wsimport.Account;
import com.zimbra.soap.account.wsimport.AuthRequest;
import com.zimbra.soap.account.wsimport.AuthResponse;
import com.zimbra.soap.account.wsimport.By;
import com.zimbra.soap.mail.wsimport.MailService_Service;
import com.zimbra.soap.mail.wsimport.MailService;

import org.junit.Assert;

/**
 * Current assumption : user1 exists with password test123
 */
public class Utility {
    private static AccountService acctSvcEIF = null;
    private static MailService mailSvcEIF = null;
    private static String authToken = null;

    public static void addSoapAuthHeader(WSBindingProvider bp) throws Exception {
        Utility.getAuthToken();
        JAXBRIContext jaxb = (JAXBRIContext) JAXBRIContext.newInstance(HeaderContext.class);
        HeaderContext hdrCtx = new HeaderContext();
        hdrCtx.setAuthToken(authToken);
        Header soapHdr = Headers.create(jaxb,hdrCtx);
        List <Header> soapHdrs = new ArrayList <Header>();
        soapHdrs.add(soapHdr);
        // See http://metro.java.net/1.5/guide/SOAP_headers.html
        // WSBindingProvider bp = (WSBindingProvider)acctSvcEIF;
        bp.setOutboundHeaders(soapHdrs);
    }

    public static String getAuthToken() throws Exception {
        Utility.getAcctSvcEIF();
        if (authToken == null) {
            Utility.getAcctSvcEIF();
            AuthRequest authReq = new AuthRequest();
            Account acct = new Account();
            acct.setBy(By.NAME);
            acct.setValue("user1");
            authReq.setAccount(acct);
            authReq.setPassword("test123");
            authReq.setPreauth(null);
            authReq.setAuthToken(null);
            // Invoke the methods.
            AuthResponse authResponse = getAcctSvcEIF().authRequest(authReq);
            Assert.assertNotNull(authResponse);
            authToken = authResponse.getAuthToken();
        }
        return authToken;
    }

    private static void setAcctSvcEIF(AccountService acctSvcEIF) {
        Utility.acctSvcEIF = acctSvcEIF;
    }

    public static AccountService getAcctSvcEIF() throws Exception {
        if (acctSvcEIF == null) {
            // The AccountService_Service class is the Java type bound to
            // the service section of the WSDL document.
            AccountService_Service acctSvc = new AccountService_Service();
            setAcctSvcEIF(acctSvc.getAccountServicePort());
        }
        return acctSvcEIF;
    }

    private static void setMailSvcEIF(MailService mailSvcEIF) {
        Utility.mailSvcEIF = mailSvcEIF;
    }

    public static MailService getMailSvcEIF() {
        if (mailSvcEIF == null) {
            MailService_Service mailSvc = new MailService_Service();
            Utility.setMailSvcEIF(mailSvc.getMailServicePort());
        }
        return mailSvcEIF;
    }
}
