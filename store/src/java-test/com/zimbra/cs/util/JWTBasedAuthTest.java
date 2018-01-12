/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.primitives.Bytes;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.JWTUtil;
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
            Assert.assertTrue(afse.getReason().equals("invalid jwt"));
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

    @Test
    public void testGenerateAndValidateJWT() {
        Account acct;
        try {
            acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            String salt = "s1";
            String salts ="s2|s3|s1";
            byte[] jwtKey = Bytes.concat(JWTUtil.getTokenKey(), salt.getBytes());
            long issuedAt = System.currentTimeMillis();
            long expires = issuedAt + 3600000;
            String jwt = JWTUtil.generateJWT(jwtKey, salt, issuedAt, expires, acct);
            Claims claims = JWTUtil.validateJWT(jwt, salts);
            Assert.assertEquals(acct.getId(), claims.getSubject());
        } catch (ServiceException e) {
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
}
