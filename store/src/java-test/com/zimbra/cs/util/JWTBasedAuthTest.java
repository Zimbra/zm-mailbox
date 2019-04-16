package com.zimbra.cs.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.primitives.Bytes;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
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
import com.zimbra.cs.account.AuthTokenProperties;
import com.zimbra.cs.account.AuthTokenUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraJWToken;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.util.JWTUtil;
import com.zimbra.cs.service.util.UserServletUtil;
import com.zimbra.cs.servlet.ZimbraQoSFilter;
import com.zimbra.soap.SoapServlet;

import io.jsonwebtoken.Claims;
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
    public void testGenerateJWTusingProvider() {
        Account acct;
        try {
            acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            AuthToken at = AuthProvider.getAuthToken(acct, TokenType.JWT);
            validateJWT(at, acct.getId());
        } catch (ServiceException e) {
            Assert.fail("testGenerateJWTusingProvider failed");
        }
    }

    @Test
    public void testAccountAndUsageJWT() {
        Account acct;
        try {
            acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            AuthToken at = AuthProvider.getAuthToken(acct, Usage.TWO_FACTOR_AUTH, TokenType.JWT);
            validateJWT(at, acct.getId());
        } catch (ServiceException e) {
            Assert.fail("testAccountAndUsageJWT failed");
       }
    }

    @Test
    public void testAccountAndExpiresJWT() {
        Account acct;
        try {
            acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            AuthToken at = AuthProvider.getAuthToken(acct, 0, TokenType.JWT);
            validateJWT(at, acct.getId());
        } catch (ServiceException e) {
            Assert.fail("testAccountAndExpiresJWT failed");
        }
    }

    // negative case
    @Test
    public void testNegativeValdiate() {
        String token = "abc.dev.xyz";
        try {
            JWTUtil.validateJWT(token, "abcd");
        } catch(AuthFailedServiceException afse) {
            Assert.assertTrue(afse.getReason().equals("Malformed JWT received"));
        } catch (ServiceException e) {
            Assert.fail("testNegativeValdiate failed");
        }
    }

    private void validateJWT(AuthToken at, String acctId) {
        String jwt;
        try {
            Element response = new Element.XMLElement(AccountConstants.AUTH_RESPONSE);
            at.encodeAuthResp(response, false);
            jwt = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
            String[] jwtClaims = jwt.split("\\.");
            String jwtBody = StringUtils.newStringUtf8(Base64.decodeBase64(jwtClaims[1]));
            Assert.assertTrue(jwtBody.contains(acctId));
        } catch (ServiceException e) {
            Assert.fail("validation failed");
        }
    }

    private String generateJWT(Account acct, String salt) throws AuthTokenException, ServiceException {
        AuthTokenKey atkey = AuthTokenUtil.getCurrentKey();
        byte[] jwtKey = Bytes.concat(atkey.getKey(), salt.getBytes());
        long issuedAt = System.currentTimeMillis();
        long expires = issuedAt + 3600000;
        AuthTokenProperties properties = new AuthTokenProperties(acct, false, null, expires, null, Usage.AUTH);
        String jwt = JWTUtil.generateJWT(jwtKey, salt, issuedAt, properties, atkey.getVersion());
        return jwt;
    }

    @Test
    public void testGenerateAndValidateJWT() {
        Account acct;
        try {
            acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            String salt = "s1";
            String salts ="s2|s3|s1";
            Claims claims = JWTUtil.validateJWT(generateJWT(acct, salt), salts);
            Assert.assertEquals(acct.getId(), claims.getSubject());
        } catch (ServiceException | AuthTokenException e) {
            e.printStackTrace();
            Assert.fail("testGenerateAndValidateJWT failed");
        }
    }

    @Test
    public void testGetSaltSoapContext() {
        Element soapCtxt = EasyMock.createMock(Element.class);
        String salt = "s1";
        EasyMock.expect(soapCtxt.getAttribute(HeaderConstants.E_JWT_SALT, null)).andReturn(salt);
        EasyMock.replay(soapCtxt);
        String saltFound = JWTUtil.getSalt(soapCtxt, null);
        Assert.assertEquals(salt, saltFound);
    }

    @Test
    public void testGetSaltCookie() {
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        Cookie cookies[] =  new Cookie[1];
        String salt = "s1";
        Cookie cookie = new Cookie("ZM_JWT", salt);
        cookie.setHttpOnly(true);
        cookies[0] = cookie;
        EasyMock.expect(req.getCookies()).andReturn(cookies);
        EasyMock.replay(req);
        Map<String, Object> engineCtxt = new HashMap<String, Object>();
        engineCtxt.put(SoapServlet.SERVLET_REQUEST, req);
        String saltFound = JWTUtil.getSalt(null, engineCtxt);
        Assert.assertEquals(salt, saltFound);
    }

    @Test
    public void testClearSalt() {
        String salt = "s1";
        String zmJWTCookieValue = "s2|s3|s1";
        Assert.assertEquals("s2|s3", JWTUtil.clearSalt(zmJWTCookieValue, salt));
        zmJWTCookieValue = "s2|s1|s3";
        Assert.assertEquals("s2|s3", JWTUtil.clearSalt(zmJWTCookieValue, salt));
        zmJWTCookieValue = "s1|s2|s3";
        Assert.assertEquals("s2|s3", JWTUtil.clearSalt(zmJWTCookieValue, salt));
        zmJWTCookieValue = "s1";
        Assert.assertEquals("", JWTUtil.clearSalt(zmJWTCookieValue, salt));
        zmJWTCookieValue = "s2|s3|s4";
        Assert.assertEquals("s2|s3|s4", JWTUtil.clearSalt(zmJWTCookieValue, salt));
    }

    @Test
    public void testGetJWToken() {
        Account acct;
        try {
            acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            String salt = "s1";
            String salts ="s2|s3|s1";
            AuthTokenKey atkey = AuthTokenUtil.getCurrentKey();
            byte[] jwtKey = Bytes.concat(atkey.getKey(), salt.getBytes());
            long issuedAt = System.currentTimeMillis();
            long expires = issuedAt + 3600000;
            AuthTokenProperties properties = new AuthTokenProperties(acct, true, null, expires, AuthMech.zimbra, Usage.AUTH);
            String jwt = JWTUtil.generateJWT(jwtKey, salt, issuedAt, properties, atkey.getVersion());
            AuthToken at = ZimbraJWToken.getJWToken(jwt, salts);
            Assert.assertEquals(acct.getId(), at.getAccountId());
            Assert.assertEquals(Usage.AUTH, at.getUsage());
            Assert.assertEquals(expires/1000, at.getExpires()/1000);
            Assert.assertEquals(AuthMech.zimbra, at.getAuthMech());
            Assert.assertEquals(false, at.isAdmin());
        } catch (ServiceException | AuthTokenException e) {
            e.printStackTrace();
            Assert.fail("testGenerateAndValidateJWT failed");
        }
    }

    @Test
    public void testGetJWTSalt() {
        Account acct;
        try {
            acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            String salt = "s1";
            String jwt = generateJWT(acct, salt);
            String jwtSalt = JWTUtil.getJWTSalt(jwt);
            Assert.assertEquals(salt, jwtSalt);
        } catch (ServiceException | AuthTokenException e) {
            e.printStackTrace();
            Assert.fail("testGetJWTSalt failed");
        }
    }

    @Test
    public void testUserServletJWTFlowWithAuthHeader() {
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            UserServletContext context = mockServletConext(acct, false);
            UserServletUtil.getAccount(context);
            Assert.assertNotNull(context.authToken);
            Assert.assertEquals(acct.getId(), context.authToken.getAccountId());
        } catch (ServiceException | AuthTokenException | IOException | ServletException | UserServletException e1) {
            e1.printStackTrace();
            Assert.fail("testUserServletJWTFlow failed");
        }
    }

    @Test
    public void testUserServletJWTFlowWithJWTQueryParam() {
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            UserServletContext context = mockServletConext(acct, true);
            UserServletUtil.getAccount(context);
            Assert.assertNotNull(context.authToken);
            Assert.assertEquals(acct.getId(), context.authToken.getAccountId());
        } catch (ServiceException | AuthTokenException | IOException | ServletException | UserServletException e1) {
            e1.printStackTrace();
            Assert.fail("testUserServletJWTFlow failed");
        }
    }

    private UserServletContext mockServletConext(Account acct, boolean queryParamFlow) throws ServiceException, AuthTokenException, UserServletException {
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        HttpServletResponse resp = EasyMock.createMock(HttpServletResponse.class);
        UserServlet usrSrv = EasyMock.createMock(UserServlet.class);
        Cookie cookies[] =  new Cookie[1];
        String salt = "s1";
        Cookie cookie = new Cookie("ZM_JWT", salt);
        cookie.setHttpOnly(true);
        cookies[0] = cookie;
        int port = 7071;
        String jwt = generateJWT(acct, salt);
        String jwtQP = queryParamFlow ? "auth=jwt&zjwt="+jwt : "auth=jwt";
        EasyMock.expect(req.getQueryString()).andReturn(jwtQP);
        EasyMock.expect(req.getPathInfo()).andReturn("/test/contacts");
        EasyMock.expect(req.getCookies()).andReturn(cookies);
        EasyMock.expect(req.getLocalPort()).andReturn(port);
        jwt = queryParamFlow ? null : "Bearer " + jwt;
        EasyMock.expect(req.getHeader(Constants.AUTH_HEADER)).andReturn(jwt);
        EasyMock.replay(req);
        UserServletContext context = new UserServletContext(req, resp, usrSrv);
        return context;
    }

    @Test
    public void testZimbraQoSFilterExtractUserId() {
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        Cookie cookies[] =  new Cookie[1];
        String salt = "s1";
        Cookie cookie = new Cookie("ZM_JWT", salt);
        cookie.setHttpOnly(true);
        cookies[0] = cookie;
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            String jwt = generateJWT(acct, salt);
            EasyMock.expect(req.getCookies()).andReturn(cookies);
            EasyMock.expectLastCall().times(2);
            EasyMock.expect(req.getHeader(Constants.AUTH_HEADER)).andReturn("Bearer " + jwt);
            int port = 7071;
            EasyMock.expect(req.getLocalPort()).andReturn(port);
            EasyMock.replay(req);
            String accountId = ZimbraQoSFilter.extractUserId(req);
            Assert.assertEquals(acct.getId(), accountId);
        } catch (ServiceException | AuthTokenException e) {
            e.printStackTrace();
            Assert.fail("testZimbraQoSFilterExtractUserId failed");
        }
    }

    @Test
    public void testJWTCookieSizeLimit() {
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        HttpServletResponse resp = EasyMock.createMock(HttpServletResponse.class);
        Cookie cookies[] =  new Cookie[1];
        String generatedSalt = RandomStringUtils.random(4095, true, true);
        Cookie cookie = new Cookie("ZM_JWT", generatedSalt);
        cookie.setHttpOnly(true);
        cookies[0] = cookie;
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            EasyMock.expect(req.getCookies()).andReturn(cookies);
            EasyMock.replay(req);
            ZimbraJWToken jwt = new ZimbraJWToken(acct);
            jwt.encode(req, resp, false);
            Assert.fail("testJWTCookieSize failed");
        } catch (AuthFailedServiceException e) {
                Assert.assertEquals("JWT Cookie size limit exceeded", e.getReason());
        } catch (ServiceException e) {
                Assert.fail("testJWTCookieSize failed");
        }
    }
}
