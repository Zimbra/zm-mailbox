package com.zimbra.cs.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.AuthTokenKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.account.Auth;
import com.zimbra.soap.SoapServlet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import junit.framework.Assert;

public class JWTBasedAuthTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Test
    public void testGenerateJWT() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, TokenType.JWT);
        validateJWT(at, acct.getId());
    }

    @Test
    public void testAccountAndUsageJWT() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, Usage.TWO_FACTOR_AUTH, TokenType.JWT);
        validateJWT(at, acct.getId());
    }

    @Test
    public void testAccountAndExpiresJWT() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, 0, TokenType.JWT);
        validateJWT(at, acct.getId());
    }

    // positive case
    @Test
    public void testValdiateAndCreateNewJwtAuthToken() throws ServiceException, AuthTokenException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, 0, TokenType.JWT);
        String token = at.getEncoded();
        Claims claims = Auth.validateJWT(token);
        Account authTokenAcct = prov.getAccountById(claims.getSubject());
        AuthToken newAt = AuthProvider.getAuthToken(authTokenAcct, TokenType.JWT);
        Assert.assertNotNull(newAt);
        Assert.assertNotSame(at, newAt);
        Assert.assertSame(acct, authTokenAcct);
        validateJWT(newAt, authTokenAcct.getId());
    }

    // negative case
    @Test
    public void testNegativeValdiateAndCreateNewJwtAuthToken() throws ServiceException, AuthTokenException {
        String token = "abc.dev.xyz";
        try {
            Auth.validateJWT(token);
        } catch(AuthFailedServiceException afse) {
            Assert.assertTrue(afse.getReason().equals("Malformed JWT received"));
        }
    }

    private void validateJWT(AuthToken at, String acctId) throws ServiceException, AuthTokenException {
        String jwt = at.getEncoded();
        AuthTokenKey tokenKey = AuthTokenKey.getCurrentKey();
        java.security.Key key = new SecretKeySpec(tokenKey.getKey(), SignatureAlgorithm.HS512.getJcaName());
        assert Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody().getSubject().equals(acctId);
    }

    @Test
    public void testJWTSoapContextUsage() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, 0, TokenType.JWT);
        String jwt = at.getEncoded();
        Element ctxt = EasyMock.createMock(Element.class);
        EasyMock.expect(ctxt.getAttribute(HeaderConstants.E_JWT_TOKEN, null)).andReturn(jwt);
        EasyMock.replay(ctxt);
        AuthToken jwt_at = AuthProvider.getAuthToken(ctxt, null, TokenType.JWT);
        assertEquals(acct.getId(), jwt_at.getAccountId());
    }

    @Test
    public void testJWTHeaderUsage() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, 0, TokenType.JWT);
        String jwt = at.getEncoded();
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getHeader(Constants.AUTH_HEADER)).andReturn("Bearer " + jwt);
        EasyMock.replay(req);
        Map<String, Object> engineCtxt = new HashMap<String, Object>();
        engineCtxt.put(SoapServlet.SERVLET_REQUEST, req);
        AuthToken jwt_at = AuthProvider.getAuthToken(null, engineCtxt, TokenType.JWT);
        assertEquals(acct.getId(), jwt_at.getAccountId());
    }
}
