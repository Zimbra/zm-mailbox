package com.zimbra.cs.util;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.AuthTokenKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.account.Auth;

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

    private void validateJWT(AuthToken at, String acctId) throws ServiceException, AuthTokenException {
        String jwt = at.getEncoded();
        AuthTokenKey tokenKey = AuthTokenKey.getCurrentKey();
        java.security.Key key = new SecretKeySpec(tokenKey.getKey(), SignatureAlgorithm.HS512.getJcaName());
        assert Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody().getSubject().equals(acctId);
    }

    // positive case
    @Test
    public void testValdiateAndCreateNewJwtAuthToken() throws ServiceException, AuthTokenException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, 0, TokenType.JWT);
        String token = at.getEncoded();
        AuthToken newAt = Auth.validateAndCreateNewJwtToken(token, acct, prov);
        Assert.assertNotNull(newAt);
        Assert.assertNotSame(at, newAt);
        String jwt = newAt.getEncoded();
        java.security.Key key = new SecretKeySpec("pass".getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());
        assert Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody().getSubject().equals(acct.getId());
    }

    // negative case
    @Test
    public void testNegativeValdiateAndCreateNewJwtAuthToken() throws ServiceException, AuthTokenException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(Key.AccountBy.name, "test@zimbra.com");
        String token = "abc.dev.xyz";
        try {
            Auth.validateAndCreateNewJwtToken(token, acct, prov);
        } catch(Exception e) {
            Assert.assertTrue(e.getMessage().contains("Malformed JWT received"));
        }
    }
}