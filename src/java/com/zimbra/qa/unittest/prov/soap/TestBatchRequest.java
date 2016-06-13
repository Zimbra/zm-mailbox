/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        SoapTransport transport = authUser(acct.getName(), csrfEnabled, Boolean.FALSE);

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

    @Test
    public void batchReqWithCsrfToken() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        boolean csrfEnabled = Boolean.TRUE;
        SoapTransport transport = authUser(acct.getName(), csrfEnabled, Boolean.TRUE);

        Element request = new Element.XMLElement(ZimbraNamespace.E_BATCH_REQUEST);
        String sigContent = "xss&lt;script&gt;alert(\"XSS\")&lt;/script&gt;&lt;a href=javascript:alert(\"XSS\")&gt;&lt;";

        Signature sig = new Signature(null, "testSig", sigContent, "text/html");
        CreateSignatureRequest req = new CreateSignatureRequest(sig);
        SoapProtocol proto = SoapProtocol.Soap12;
        Element sigReq = JaxbUtil.jaxbToElement(req, proto.getFactory());
        request.addElement(sigReq);
        try {
            Element sigResp = transport.invoke(request, false, false, null);
            String sigt = sigResp.getElement("CreateSignatureResponse").getElement("signature").getAttribute("id");
            assertNotNull(sigt);
        } catch (SoapFaultException e) {
            assertNull(e);

        }
    }

}
