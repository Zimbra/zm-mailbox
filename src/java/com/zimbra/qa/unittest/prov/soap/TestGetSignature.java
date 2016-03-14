/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2013, 2014 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.CreateSignatureRequest;
import com.zimbra.soap.account.message.GetSignaturesRequest;
import com.zimbra.soap.account.type.Signature;


/**
 * @author zimbra
 *
 */
public class TestGetSignature extends SoapTest {
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
    public void getSignature() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        boolean csrfEnabled = Boolean.FALSE;
        SoapTransport transport = authUser(acct.getName(), csrfEnabled, Boolean.FALSE);


        String sigContent = "xss&lt;script&gt;alert(\"XSS\")&lt;/script&gt;&lt;a href=javascript:alert(\"XSS\")&gt;&lt;";

        Signature sig = new Signature(null, "testSig", sigContent, "text/html");
        CreateSignatureRequest req = new CreateSignatureRequest(sig);
        SoapProtocol proto = SoapProtocol.Soap12;
        Element sigReq = JaxbUtil.jaxbToElement(req, proto.getFactory());

        try {
            Element element = transport.invoke(sigReq, false, false, null);
            String sigt = element.getElement("signature").getAttribute("id");
            assertNotNull(sigt);
        } catch (SoapFaultException e) {
            e.printStackTrace();
            assertNull(e);
        }

        GetSignaturesRequest getSigReq = new GetSignaturesRequest();
        sigReq = JaxbUtil.jaxbToElement(getSigReq, proto.getFactory());
        try {
            Element element = transport.invoke(sigReq, false, false, null);
            String sigtContent = element.getElement("signature").getElement("content").getText();
            assertNotNull(sigContent);
            int index = sigtContent.indexOf("alert(\"XSS\")");
            Assert.assertEquals(-1, index);
        } catch (SoapFaultException e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void getSignaturePlainSig() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        boolean csrfEnabled = Boolean.FALSE;
        SoapTransport transport = authUser(acct.getName(), csrfEnabled, Boolean.FALSE);


        String sigContent = "xss&lt;script&gt;alert(\"XSS\")&lt;/script&gt;&lt;a href=javascript:alert(\"XSS\")&gt;&lt;";

        Signature sig = new Signature(null, "testSig", sigContent, "text/plain");
        CreateSignatureRequest req = new CreateSignatureRequest(sig);
        SoapProtocol proto = SoapProtocol.Soap12;
        Element sigReq = JaxbUtil.jaxbToElement(req, proto.getFactory());

        try {
            Element element = transport.invoke(sigReq, false, false, null);
            String sigt = element.getElement("signature").getAttribute("id");
            assertNotNull(sigt);
        } catch (SoapFaultException e) {
            e.printStackTrace();
            assertNull(e);
        }

        GetSignaturesRequest getSigReq = new GetSignaturesRequest();
        sigReq = JaxbUtil.jaxbToElement(getSigReq, proto.getFactory());
        try {
            Element element = transport.invoke(sigReq, false, false, null);
            String sigtContent = element.getElement("signature").getElement("content").getText();
            assertNotNull(sigContent);
            int index = sigtContent.indexOf("alert(\"XSS\")");
            Assert.assertTrue(index > -1);
        } catch (SoapFaultException e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

}
