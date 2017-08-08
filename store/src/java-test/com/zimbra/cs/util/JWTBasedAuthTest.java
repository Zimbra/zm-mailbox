package com.zimbra.cs.util;

import java.nio.charset.StandardCharsets;
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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JWTBasedAuthTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraJWTKey, "pass");
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Test
    public void testGenerateJWT() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, TokenType.JWT);
        String jwt = at.getEncoded();
        java.security.Key key = new SecretKeySpec("pass".getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());
        assert Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody().getSubject().equals(acct.getId());
    }

    @Test
    public void testAccountAndUsageJWT() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, Usage.TWO_FACTOR_AUTH, TokenType.JWT);
        String jwt = at.getEncoded();
        java.security.Key key = new SecretKeySpec("pass".getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());
        assert Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody().getSubject().equals(acct.getId());
    }

    @Test
    public void testAccountAndExpiresJWT() throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        AuthToken at = AuthProvider.getAuthToken(acct, 0, TokenType.JWT);
        String jwt = at.getEncoded();
        java.security.Key key = new SecretKeySpec("pass".getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());
        assert Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody().getSubject().equals(acct.getId());
    }
}
