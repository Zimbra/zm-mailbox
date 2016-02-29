/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2013, 2014 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.assertNotNull;
import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.CreateSignatureRequest;
import com.zimbra.soap.account.type.Signature;


/**
 * @author zimbra
 *
 */
public class TestBatchRequest extends SoapTest {
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }

    @Test
    public void batchReqWithoutCsrfToken() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        boolean csrfEnabled = Boolean.TRUE;
        SoapTransport transport = authUser(acct.getName(), csrfEnabled);

        Element request = new Element.XMLElement(ZimbraNamespace.E_BATCH_REQUEST);
        String sigContent = "xss&lt;script&gt;alert(\"XSS\")&lt;/script&gt;&lt;a href=javascript:alert(\"XSS\")&gt;&lt;";

        Signature sig = new Signature("test_id", "testSig", sigContent, "text/html");
        CreateSignatureRequest req = new CreateSignatureRequest(sig);
        SoapProtocol proto = SoapProtocol.Soap12;
        Element sigReq = JaxbUtil.jaxbToElement(req, proto.getFactory());
        request.addElement(sigReq);
        try {
            transport.invoke(request, false, false, null);
        } catch (SoapFaultException e) {
            assertNotNull(e);
            Assert.assertEquals(true, e.getCode().contains("AUTH_REQUIRED"));
        }
    }

}
