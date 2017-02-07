package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralStoreTest.TestLocation;

public class AttributeEncodersTest {

    @Test
    public void testKeyEncoders() throws Exception {
        EphemeralLocation location = new TestLocation();
        EphemeralKey staticKey = new EphemeralKey("foo");
        EphemeralKey dynamicKey = new EphemeralKey("foo", "1");

        //test static key encoder
        KeyEncoder staticEncoder = new StaticKeyEncoder();
        assertEquals("foo", staticEncoder.encodeKey(staticKey, location));
        assertEquals("foo", staticEncoder.encodeKey(dynamicKey, location));
    }

    @Test
    public void testValueEncoders() throws Exception {
        EphemeralLocation location = new TestLocation();
        EphemeralKey staticKey = new EphemeralKey("foo");
        EphemeralKey dynamicKey = new EphemeralKey("foo", "1");
        ValueEncoder dynamicExpirationEncoder = new DynamicExpirationValueEncoder();
        EphemeralInput staticInput = new EphemeralInput(staticKey, "bar");
        EphemeralInput dynamicInput = new EphemeralInput(dynamicKey, "bar");

        //static key, no expiration
        assertEquals("bar", dynamicExpirationEncoder.encodeValue(staticInput, location));

        //dynamic key, no expiration
        assertEquals("bar|1|", dynamicExpirationEncoder.encodeValue(dynamicInput, location));


        //set expirations
        staticInput.setExpiration(new AbsoluteExpiration(1000L));
        dynamicInput.setExpiration(new AbsoluteExpiration(1000L));

        //static key, expiration
        assertEquals("bar||1000", dynamicExpirationEncoder.encodeValue(staticInput, location));

        //dynamic key, expiration
        assertEquals("bar|1|1000", dynamicExpirationEncoder.encodeValue(dynamicInput, location));

        //test special handling of auth and CSRF token encoding for LDAP backwards compatibility
        ValueEncoder ldapValueEncoder = new LdapValueEncoder();
        EphemeralKey authKey = new EphemeralKey(Provisioning.A_zimbraAuthTokens, "tokenId");
        EphemeralInput authInput = new EphemeralInput(authKey, "serverVersion");
        authInput.setExpiration(new AbsoluteExpiration(1000L));
        assertEquals("tokenId|1000|serverVersion", ldapValueEncoder.encodeValue(authInput, location));

        EphemeralKey csrfKey = new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, "crumb");
        EphemeralInput csrfInput = new EphemeralInput(csrfKey, "data");
        csrfInput.setExpiration(new AbsoluteExpiration(1000L));
        assertEquals("data:crumb:1000", ldapValueEncoder.encodeValue(csrfInput, location));
    }


    @Test
    public void testLdapEncoder() throws Exception {
        DynamicExpirationEncoder encoder = new LdapAttributeEncoder();
        verifyKeyValuePair(encoder.decode("foo", "bar"), "foo", null, "bar", null);
        verifyKeyValuePair(encoder.decode("foo", "bar||1000"), "foo", null, "bar", 1000L);
        verifyKeyValuePair(encoder.decode("foo", "bar|1|1000"), "foo", "1", "bar", 1000L);
        verifyKeyValuePair(encoder.decode("foo", "bar|1|"), "foo", "1", "bar", null);
        verifyKeyValuePair(encoder.decode("foo", "bar|1|bad"), "foo", null, "bar|1|bad", null);
        verifyKeyValuePair(encoder.decode("foo", "bar|1000"), "foo", null, "bar|1000", null);
        verifyKeyValuePair(encoder.decode("foo", "b|a|r|1|1000"), "foo", "1", "b|a|r", 1000L);

        //test special handling of auth token
        ExpirableEphemeralKeyValuePair kvp = encoder.decode(Provisioning.A_zimbraAuthTokens, "tokenId|1000|serverVersion");
        verifyKeyValuePair(kvp, Provisioning.A_zimbraAuthTokens, "tokenId", "serverVersion", 1000L);

        //test special handling of csrf token
        kvp = encoder.decode(Provisioning.A_zimbraCsrfTokenData, "data:crumb:1000");
        verifyKeyValuePair(kvp, Provisioning.A_zimbraCsrfTokenData, "crumb", "data", 1000L);
    }

    private void verifyKeyValuePair(ExpirableEphemeralKeyValuePair kvp, String mainKeyPart, String dynamicKeyPart, String value, Long expiration) {
        EphemeralKey key  = kvp.getKey();
        assertEquals(mainKeyPart, key.getKey());
        if (dynamicKeyPart == null) {
            assertFalse(key.isDynamic());
        } else {
            assertEquals(dynamicKeyPart, key.getDynamicComponent());
        }
        assertEquals(value, kvp.getValue());
        if (expiration == null) {
            assertNull(kvp.getExpiration());
        } else {
            assertEquals(expiration, kvp.getExpiration());
        }
    }
}
