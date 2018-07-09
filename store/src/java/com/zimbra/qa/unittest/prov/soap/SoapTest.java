/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;

import org.apache.http.HttpException;
import org.junit.BeforeClass;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.LocalconfigTestUtil;
import com.zimbra.qa.unittest.prov.ProvTest;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.admin.message.ReloadLocalConfigRequest;
import com.zimbra.soap.admin.message.ReloadLocalConfigResponse;

public class SoapTest extends ProvTest {
    private static boolean JSON = false;

    private static final String SOAP_TEST_BASE_DOMAIN = "soaptest";

    private static String PASSWORD = "test123";
    private static HttpDebugListener soapDebugListener;

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (!TestUtil.fromRunUnitTests) {
            CliUtil.toolSetup(); // init ssl stuff
        }
        soapDebugListener = new SoapDebugListener();

        // init rights
        RightManager.getInstance();
    }

    public static String baseDomainName() {
        StackTraceElement [] s = new RuntimeException().getStackTrace();
        return s[1].getClassName().toLowerCase() + "." +
                SOAP_TEST_BASE_DOMAIN + "." + InMemoryLdapServer.UNITTEST_BASE_DOMAIN_SEGMENT;
    }

    static SoapTransport authUser(String acctName) throws Exception {
        return authUser(acctName, PASSWORD);
    }

    static SoapTransport authUser(String acctName, String password) throws Exception {
        com.zimbra.soap.type.AccountSelector acct =
            new com.zimbra.soap.type.AccountSelector(com.zimbra.soap.type.AccountBy.name, acctName);

        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setHttpDebugListener(soapDebugListener);

        AuthRequest req = new AuthRequest(acct, password);
        AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        return transport;
    }

    /**
     * @param name
     * @param csrfEnabled
     * @return
     * @throws IOException
     * @throws ServiceException
     */
   public static SoapTransport authUser(String acctName, boolean csrfEnabled, boolean setCsrfToken) 
       throws ServiceException, IOException, HttpException {
        com.zimbra.soap.type.AccountSelector acct =
            new com.zimbra.soap.type.AccountSelector(com.zimbra.soap.type.AccountBy.name, acctName);

        SoapHttpTransport transport = new SoapHttpTransport("http://localhost:7070/service/soap/");
        transport.setHttpDebugListener(soapDebugListener);

        AuthRequest req = new AuthRequest(acct, PASSWORD);
        req.setCsrfSupported(csrfEnabled);
        AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        if (setCsrfToken) {
            transport.setCsrfToken(resp.getCsrfToken());
        }
        return transport;
    }

    public static SoapTransport authAdmin(String acctName) throws Exception {
        return authAdmin(acctName, PASSWORD);
    }

    public static SoapTransport authAdmin(String acctName, String password) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        transport.setHttpDebugListener(soapDebugListener);

        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(acctName, password);
        com.zimbra.soap.admin.message.AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        return transport;
    }

    static SoapTransport authZimbraAdmin() throws Exception {
        return authAdmin(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
    }

    static void modifyLocalconfigAndReload(SoapTransport transport, KnownKey key, String value)
    throws Exception {
        LocalconfigTestUtil.modifyLocalConfig(key, value);

        // reload LC on server
        ReloadLocalConfigRequest req = new ReloadLocalConfigRequest();
        ReloadLocalConfigResponse resp = invokeJaxb(transport, req);
    }

    public static <T> T invokeJaxb(SoapTransport transport, Object jaxbObject)
    throws ServiceException, IOException, HttpException {
        return (T) invokeJaxb(transport, jaxbObject,
                JSON ? SoapProtocol.SoapJS : SoapProtocol.Soap12);
    }

    public static <T> T invokeJaxb(SoapTransport transport, Object jaxbObject, SoapProtocol proto)
    throws ServiceException, IOException, HttpException {
        Element req = JaxbUtil.jaxbToElement(jaxbObject, proto.getFactory());

        Element res = transport.invoke(req);
        return (T) JaxbUtil.elementToJaxb(res);
    }

    public static <T> T invokeJaxbOnTargetAccount(SoapTransport transport, Object jaxbObject,
            String targetAcctId)
    throws Exception {
        String oldTarget = transport.getTargetAcctId();
        try {
            transport.setTargetAcctId(targetAcctId);
            return (T) invokeJaxb(transport, jaxbObject);
        } finally {
            transport.setTargetAcctId(oldTarget);
        }
    }
}
