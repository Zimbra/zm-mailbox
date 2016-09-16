package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCardinality;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeType;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.RelativeExpiration;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class EphemeralAttributesTest {
    private static Account acct;
    private static EphemeralStore store;
    private static EphemeralLocation location;

    @BeforeClass
    public static void init() throws Exception {
        initEphemeralAttributes();
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        acct = prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        store = EphemeralStore.getFactory().getStore();
        location = new LdapEntryLocation(acct);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        EphemeralStore.getFactory().shutdown();
    }

    @Test
    public void testCsrfTokens() throws Exception {
        EphemeralKey tokenDataKey1 = new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, "crumb1");
        EphemeralKey tokenDataKey2 = new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, "crumb2");
        acct.addCsrfTokenData("crumb1", "data1", new AbsoluteExpiration(1000L)); //already expired
        acct.addCsrfTokenData("crumb2", "data2", new RelativeExpiration(1L, TimeUnit.DAYS));

        //is it in the ephemeral store?
        assertEquals("data1", store.get(tokenDataKey1, location).getValue());
        assertEquals("data2", store.get(tokenDataKey2, location).getValue());

        //test getter
        assertEquals("data1", acct.getCsrfTokenData("crumb1"));
        assertEquals("data2", acct.getCsrfTokenData("crumb2"));

        //test removing expired
        acct.purgeCsrfTokenData();
        assertTrue(store.get(tokenDataKey1, location).isEmpty());
        assertEquals("data2", store.get(tokenDataKey2, location).getValue());

        //test removing a specific value
        acct.addCsrfTokenData("crumb3", "data3", new RelativeExpiration(1L, TimeUnit.DAYS));
        acct.removeCsrfTokenData("crumb2", "data2");
        assertTrue(store.get(tokenDataKey2, location).isEmpty());
        assertEquals("data3", acct.getCsrfTokenData("crumb3"));

        //test removing incorrect value
        acct.removeCsrfTokenData("crumb3", "data2");
        assertEquals("data3", acct.getCsrfTokenData("crumb3"));
    }

    @Test
    public void testAuthTokens() throws Exception {
        EphemeralKey tokenKey1 = new EphemeralKey(Provisioning.A_zimbraAuthTokens, "token1");
        EphemeralKey tokenKey2 = new EphemeralKey(Provisioning.A_zimbraAuthTokens, "token2");
        acct.addAuthTokens("token1", "data1", new AbsoluteExpiration(1000L)); //already expired
        acct.addAuthTokens("token2", "data2", new RelativeExpiration(1L, TimeUnit.DAYS));

        //is it in the ephemeral store?
        assertEquals("data1", store.get(tokenKey1, location).getValue());
        assertEquals("data2", store.get(tokenKey2, location).getValue());

        //test getter
        assertEquals("data1", acct.getAuthTokens("token1"));
        assertEquals("data2", acct.getAuthTokens("token2"));

        //test removing expired
        acct.purgeAuthTokens();
        assertTrue(store.get(tokenKey1, location).isEmpty());
        assertEquals("data2", store.get(tokenKey2, location).getValue());

        //test removing a specific value
        acct.addAuthTokens("token3","data3", new RelativeExpiration(1L, TimeUnit.DAYS));
        acct.removeAuthTokens("token2", "data2");
        assertTrue(store.get(tokenKey2, location).isEmpty());
        assertEquals("data3", acct.getAuthTokens("token3"));

        //test removing incorrect value
        acct.removeAuthTokens("token3", "data2");
        assertEquals("data3", acct.getAuthTokens("token3"));
    }

    @Test
    public void testLastLogonTimestamp() throws Exception {
        EphemeralKey key = new EphemeralKey(Provisioning.A_zimbraLastLogonTimestamp);
        Date login1 = new Date();
        acct.setLastLogonTimestamp(login1);
        Date login2 = new Date();
        acct.setLastLogonTimestamp(login2);

        //is it in the ephemeral store?
        EphemeralResult result = store.get(key, location);
        assertEquals(1, result.getValues().length);
        assertEquals(LdapDateUtil.toGeneralizedTime(login2), result.getValue());

        //test the getter
        assertEquals(LdapDateUtil.toGeneralizedTime(login2), acct.getLastLogonTimestampAsString());
        acct.unsetLastLogonTimestamp();
        assertTrue(store.get(key, location).isEmpty());
    }

    private static void initEphemeralAttributes() throws Exception {
        Set<AttributeClass> requiredIn = Sets.newHashSet(AttributeClass.account);
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral, AttributeFlag.dynamic, AttributeFlag.expirable);
        AttributeInfo ai1 = new AttributeInfo(Provisioning.A_zimbraAuthTokens, 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.multi, requiredIn, null, flags, null, null, null, null, null, "auth tokens", null, null, null);
        AttributeInfo ai2 = new AttributeInfo(Provisioning.A_zimbraCsrfTokenData, 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.multi, requiredIn, null, flags, null, null, null, null, null, "csrf tokens", null, null, null);
        AttributeInfo ai3 = new AttributeInfo(Provisioning.A_zimbraLastLogonTimestamp, 1, null, 0, null, AttributeType.TYPE_GENTIME, null, "", true, null, null, AttributeCardinality.single, requiredIn, null, flags, null, null, null, null, null, "last logon timestamp", null, null, null);
        AttributeInfo ai4 = new AttributeInfo(Provisioning.A_zimbraAppSpecificPassword, 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.single, requiredIn, null, flags, null, null, null, null, null, "app-specific passwords", null, null, null);
        AttributeManager am = new AttributeManager();
        am.addAttribute(ai1);
        am.addAttribute(ai2);
        am.addAttribute(ai3);
        am.addAttribute(ai4);
    }
}
