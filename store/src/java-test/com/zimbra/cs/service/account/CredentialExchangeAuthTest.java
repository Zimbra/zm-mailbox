/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.service.account;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.MailMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.CredentialExchangeService;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

public class CredentialExchangeAuthTest {

    private static final String DOMAIN = "example.com";

    private static final String VIRTUAL_HOST = "mail.example.com";

    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    private static final MockProvisioning PROV = new MockProvisioning();

    private Account alice;

    private Account bob;

    private Map<String, Object> requestAttrs;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.setInstance(PROV);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        final Map<String, Object> domainAttrs = new HashMap<>();
        domainAttrs.put(Provisioning.A_zimbraWebClientLoginURL, "https://mail.example.com/service/extension/samllogin");
        domainAttrs.put(Provisioning.A_zimbraSamlSSOURL, "https://idp.example.com/sso");
        domainAttrs.put(Provisioning.A_zimbraVirtualHostname, VIRTUAL_HOST);
        PROV.createDomain(DOMAIN, domainAttrs);
        alice = createAccount("alice");
        bob = createAccount("bob");
        requestAttrs = new HashMap<>();
    }

    @Test
    public void redeemsCredentialAndReturnsAuthResponse() throws Exception {
        final Element resp = handle("https", request(issue(alice), VERIFIER, "name", alice.getName(), null));
        Assert.assertEquals(AccountConstants.CREDENTIAL_EXCHANGE_AUTH_RESPONSE, resp.getQName());
        final Element token = resp.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
        Assert.assertNotNull("AuthResponse carries an authToken", token);
        Assert.assertFalse(token.getText().isEmpty());
        Assert.assertNotNull("the CSRF filter sees the new token", requestAttrs.get(CsrfFilter.AUTH_TOKEN));
    }

    @Test
    public void credentialIsSingleUse() throws Exception {
        final String credential = issue(alice);
        handle("https", request(credential, VERIFIER, "name", alice.getName(), null));
        assertAuthFailed(request(credential, VERIFIER, "name", alice.getName(), null));
    }

    @Test
    public void selectorDefaultsToName() throws Exception {
        final Element resp = handle("https", request(issue(alice), VERIFIER, null, alice.getName(), null));
        Assert.assertEquals(AccountConstants.CREDENTIAL_EXCHANGE_AUTH_RESPONSE, resp.getQName());
    }

    @Test
    public void accountCanBeSelectedById() throws Exception {
        final Element resp = handle("https", request(issue(alice), VERIFIER, "id", alice.getId(), null));
        Assert.assertEquals(AccountConstants.CREDENTIAL_EXCHANGE_AUTH_RESPONSE, resp.getQName());
    }

    @Test
    public void bareNameIsCompletedFromVirtualHost() throws Exception {
        final Domain domain = PROV.get(Key.DomainBy.name, DOMAIN);
        final Provisioning withVirtualHost = Mockito.mock(Provisioning.class, AdditionalAnswers.delegatesTo(PROV));
        Mockito.doReturn(domain).when(withVirtualHost)
                .get(Mockito.eq(Key.DomainBy.virtualHostname), Mockito.anyString());
        final Map<String, Object> ctx = context("https");
        final Element req = request(issue(alice), VERIFIER, "name", "alice", VIRTUAL_HOST);
        final Element resp;
        Provisioning.setInstance(withVirtualHost);
        try {
            resp = new CredentialExchangeAuth().handle(req, ctx);
        } finally {
            Provisioning.setInstance(PROV);
        }
        Assert.assertEquals(AccountConstants.CREDENTIAL_EXCHANGE_AUTH_RESPONSE, resp.getQName());
    }

    @Test
    public void otherAccountBurnsTheCredential() throws Exception {
        final String credential = issue(alice);
        assertAuthFailed(request(credential, VERIFIER, "name", bob.getName(), null));
        assertAuthFailed(request(credential, VERIFIER, "name", alice.getName(), null));
    }

    @Test
    public void invalidSelectorBurnsTheCredential() throws Exception {
        final String credential = issue(alice);
        assertAuthFailed(request(credential, VERIFIER, "bogus", alice.getName(), null));
        assertAuthFailed(request(credential, VERIFIER, "name", alice.getName(), null));
    }

    @Test
    public void unknownAccountBurnsTheCredential() throws Exception {
        final String credential = issue(alice);
        assertAuthFailed(request(credential, VERIFIER, "name", "nobody@" + DOMAIN, null));
        assertAuthFailed(request(credential, VERIFIER, "name", alice.getName(), null));
    }

    @Test
    public void missingAccountBurnsTheCredential() throws Exception {
        final String credential = issue(alice);
        assertAuthFailed(request(credential, VERIFIER, null, null, null));
        assertAuthFailed(request(credential, VERIFIER, "name", alice.getName(), null));
    }

    @Test
    public void lookupErrorDoesNotBurnTheCredential() throws Exception {
        final String credential = issue(alice);
        final Map<String, Object> ctx = context("https");
        final Element req = request(credential, VERIFIER, "name", alice.getName(), null);
        Provisioning.setInstance(failingFirstLookup());
        try {
            assertCode(ServiceException.FAILURE, () -> new CredentialExchangeAuth().handle(req, ctx));
        } finally {
            Provisioning.setInstance(PROV);
        }
        final Element resp = handle("https", request(credential, VERIFIER, "name", alice.getName(), null));
        Assert.assertEquals("the same credential still redeems",
                AccountConstants.CREDENTIAL_EXCHANGE_AUTH_RESPONSE, resp.getQName());
    }

    @Test
    public void malformedCredentialIsRejectedBeforeAnyLookup() throws Exception {
        final Provisioning watched = Mockito.mock(Provisioning.class, AdditionalAnswers.delegatesTo(PROV));
        final Map<String, Object> ctx = context("https");
        final Element req = request("zcx1.short", VERIFIER, "name", alice.getName(), null);
        Provisioning.setInstance(watched);
        try {
            assertAuthFailedWith(req, ctx);
        } finally {
            Provisioning.setInstance(PROV);
        }
        Mockito.verify(watched, Mockito.never()).get(Mockito.any(AccountBy.class), Mockito.anyString());
    }

    @Test
    public void cleartextIsRejectedAndNothingIsBurned() throws Exception {
        final String credential = issue(alice);
        final Server server = PROV.getLocalServer();
        server.setMailMode(MailMode.mixed);
        server.setMailClearTextPasswordEnabled(false);
        try {
            assertCode(ServiceException.INVALID_REQUEST,
                    () -> handle("http", request(credential, VERIFIER, "name", alice.getName(), null)));
        } finally {
            server.unsetMailMode();
            server.unsetMailClearTextPasswordEnabled();
        }
        final Element resp = handle("https", request(credential, VERIFIER, "name", alice.getName(), null));
        Assert.assertEquals("the rejected request did not burn the credential",
                AccountConstants.CREDENTIAL_EXCHANGE_AUTH_RESPONSE, resp.getQName());
    }

    private Account createAccount(String local) throws ServiceException {
        final Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        return PROV.createAccount(local + "@" + DOMAIN, "Passw0rd!", attrs);
    }

    private static String challengeOf(String verifier) throws Exception {
        final byte[] h = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
    }

    private static String issue(Account account) throws Exception {
        return CredentialExchangeService.getInstance().issueCredential(account, challengeOf(VERIFIER));
    }

    private static Element request(String credential, String verifier, String by, String account,
                                   String virtualHost) {
        final Element req = new Element.XMLElement(AccountConstants.CREDENTIAL_EXCHANGE_AUTH_REQUEST);
        req.addNonUniqueElement(AccountConstants.E_CREDENTIAL).setText(credential);
        req.addNonUniqueElement(AccountConstants.E_VERIFIER).setText(verifier);
        if (account != null) {
            final Element acct = req.addNonUniqueElement(AccountConstants.E_ACCOUNT).setText(account);
            if (by != null) {
                acct.addAttribute(AccountConstants.A_BY, by);
            }
        }
        if (virtualHost != null) {
            req.addNonUniqueElement(AccountConstants.E_VIRTUAL_HOST).setText(virtualHost);
        }
        return req;
    }

    private Element handle(String scheme, Element request) throws Exception {
        return new CredentialExchangeAuth().handle(request, context(scheme));
    }

    private void assertAuthFailed(Element request) throws Exception {
        assertCode(AuthFailedServiceException.AUTH_FAILED, () -> handle("https", request));
    }

    private static void assertCode(String code, ThrowingRunnable action) throws Exception {
        try {
            action.run();
            Assert.fail("expected " + code);
        } catch (ServiceException expected) {
            Assert.assertEquals(code, expected.getCode());
        }
    }

    private Map<String, Object> context(String scheme) throws Exception {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(alice), alice.getId(),
                SoapProtocol.Soap12, SoapProtocol.Soap12));
        ctx.put(SoapServlet.SERVLET_REQUEST, httpRequest(scheme, requestAttrs));
        ctx.put(SoapServlet.SERVLET_RESPONSE, httpResponse());
        ctx.put(SoapEngine.REQUEST_IP, "127.0.0.1");
        return ctx;
    }

    private static HttpServletRequest httpRequest(String scheme, Map<String, Object> attrs) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getScheme":
                            return scheme;
                        case "isSecure":
                            return "https".equals(scheme);
                        case "getAttribute":
                            return attrs.get((String) args[0]);
                        case "setAttribute":
                            attrs.put((String) args[0], args[1]);
                            return null;
                        case "getRemoteAddr":
                            return "127.0.0.1";
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
    }

    private static HttpServletResponse httpResponse() {
        return (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        return type.isPrimitive() && type != void.class ? Array.get(Array.newInstance(type, 1), 0) : null;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static Provisioning failingFirstLookup() throws ServiceException {
        final Provisioning flaky = Mockito.mock(Provisioning.class, AdditionalAnswers.delegatesTo(PROV));
        Mockito.doThrow(ServiceException.FAILURE("simulated directory failure", null))
                .doAnswer(AdditionalAnswers.delegatesTo(PROV))
                .when(flaky).get(Mockito.any(AccountBy.class), Mockito.anyString());
        return flaky;
    }

    private static void assertAuthFailedWith(Element request, Map<String, Object> ctx) throws Exception {
        assertCode(AuthFailedServiceException.AUTH_FAILED, () -> new CredentialExchangeAuth().handle(request, ctx));
    }
}
