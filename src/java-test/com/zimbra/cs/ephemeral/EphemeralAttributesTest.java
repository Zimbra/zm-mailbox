package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

        acct.addCsrfTokenData("value1", new AbsoluteExpiration(1000L)); //already expired
        acct.addCsrfTokenData("value2", new RelativeExpiration(1L, TimeUnit.DAYS));

        //is it in the ephemeral store?
        EphemeralResult result = store.get(Provisioning.A_zimbraCsrfTokenData, location);
        String[] values = result.getValues();
        assertEquals(2, values.length);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);

        //test getter
        values = acct.getCsrfTokenData();
        assertEquals(2, values.length);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);

        //test removing expired
        acct.purgeCsrfTokenData();
        result = store.get(Provisioning.A_zimbraCsrfTokenData, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value2", result.getValue());

        //test removing a specific value
        acct.addCsrfTokenData("value3", new RelativeExpiration(1L, TimeUnit.DAYS));
        acct.removeCsrfTokenData("value2");
        result = store.get(Provisioning.A_zimbraCsrfTokenData, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value3", result.getValue());

        //test setter
        acct.setCsrfTokenData(new String[] {"value4"}, new RelativeExpiration(1L, TimeUnit.DAYS));
        result = store.get(Provisioning.A_zimbraCsrfTokenData, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value4", result.getValue());

        //test removing all values
        acct.unsetCsrfTokenData();
        assertTrue(store.get(Provisioning.A_zimbraCsrfTokenData, location).isEmpty());
    }

    @Test
    public void testAppSpecificPasswords() throws Exception {
        acct.addAppSpecificPassword("value1");
        acct.addAppSpecificPassword("value2");

        //is it in the ephemeral store?
        EphemeralResult result = store.get(Provisioning.A_zimbraAppSpecificPassword, location);
        String[] values = result.getValues();
        assertEquals(2, values.length);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);

        //test getter
        values = acct.getAppSpecificPassword();
        assertEquals(2, values.length);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);

        //test removing a specific value
        acct.removeAppSpecificPassword("value1");
        result = store.get(Provisioning.A_zimbraAppSpecificPassword, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value2", result.getValue());

        //test setter
        acct.setAppSpecificPassword(new String[] {"value3"});
        result = store.get(Provisioning.A_zimbraAppSpecificPassword, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value3", result.getValue());

        //test removing all values
        acct.unsetAppSpecificPassword();
        assertTrue(store.get(Provisioning.A_zimbraAppSpecificPassword, location).isEmpty());
    }

    @Test
    public void testAuthTokens() throws Exception {
        acct.addAuthTokens("value1", new AbsoluteExpiration(1000L)); //already expired
        acct.addAuthTokens("value2", new RelativeExpiration(1L, TimeUnit.DAYS));

        //is it in the ephemeral store?
        EphemeralResult result = store.get(Provisioning.A_zimbraAuthTokens, location);
        String[] values = result.getValues();
        assertEquals(2, values.length);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);

        //test getter
        values = acct.getAuthTokens();
        assertEquals(2, values.length);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);

        //test removing expired
        acct.purgeAuthTokens();
        result = store.get(Provisioning.A_zimbraAuthTokens, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value2", result.getValue());

        //test removing a specific value
        acct.addAuthTokens("value3", new RelativeExpiration(1L, TimeUnit.DAYS));
        acct.removeAuthTokens("value2");
        result = store.get(Provisioning.A_zimbraAuthTokens, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value3", result.getValue());

        //test setter
        acct.setAuthTokens(new String[] {"value4"}, new RelativeExpiration(1L, TimeUnit.DAYS));
        result = store.get(Provisioning.A_zimbraAuthTokens, location);
        assertEquals(1, result.getValues().length);
        assertEquals("value4", result.getValue());

        //test removing all values
        acct.unsetAuthTokens();
        assertTrue(store.get(Provisioning.A_zimbraAuthTokens, location).isEmpty());
    }

    @Test
    public void testLastLogonTimestamp() throws Exception {
        Date login1 = new Date();
        acct.setLastLogonTimestamp(login1);
        Date login2 = new Date();
        acct.setLastLogonTimestamp(login2);

        //is it in the ephemeral store?
        EphemeralResult result = store.get(Provisioning.A_zimbraLastLogonTimestamp, location);
        assertEquals(1, result.getValues().length);
        assertEquals(LdapDateUtil.toGeneralizedTime(login2), result.getValue());

        //test the getter
        assertEquals(LdapDateUtil.toGeneralizedTime(login2), acct.getLastLogonTimestampAsString());
        acct.unsetLastLogonTimestamp();
        assertTrue(store.get(Provisioning.A_zimbraLastLogonTimestamp, location).isEmpty());
    }

    @Test
    public void testGetEphemeralAttrs() throws Exception {
        acct.addAuthTokens("value1", new AbsoluteExpiration(1000L)); //already expired
        acct.addAppSpecificPassword("value1");
        acct.addCsrfTokenData("value1", new AbsoluteExpiration(1000L));
        acct.setLastLogonTimestamp(new Date());
        Map<String, Object> attrs = acct.getEphemeralAttrs();
        assertEquals(4, attrs.size());
    }

    private static void initEphemeralAttributes() throws Exception {
        Set<AttributeClass> requiredIn = Sets.newHashSet(AttributeClass.account);
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral, AttributeFlag.dynamic, AttributeFlag.expirable);
        AttributeInfo ai1 = new AttributeInfo("zimbraAuthTokens", 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.multi, requiredIn, null, flags, null, null, null, null, null, "auth tokens", null, null, null);
        AttributeInfo ai2 = new AttributeInfo("zimbraCsrfTokenData", 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.multi, requiredIn, null, flags, null, null, null, null, null, "csrf tokens", null, null, null);
        AttributeInfo ai3 = new AttributeInfo("zimbraLastLogonTimestamp", 1, null, 0, null, AttributeType.TYPE_GENTIME, null, "", true, null, null, AttributeCardinality.single, requiredIn, null, flags, null, null, null, null, null, "last logon timestamp", null, null, null);
        AttributeInfo ai4 = new AttributeInfo("zimbraAppSpecificPassword", 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.single, requiredIn, null, flags, null, null, null, null, null, "app-specific passwords", null, null, null);
        AttributeManager am = new AttributeManager();
        am.addAttribute(ai1);
        am.addAttribute(ai2);
        am.addAttribute(ai3);
        am.addAttribute(ai4);
    }
}
