package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.ephemeral.EphemeralStoreTest.TestLocation;

public class AttributeEncodersTest {

    //Absolute expiration; does not add System.currentTimeMillis() to passed-in value
    static class TestExpiration extends Expiration {

        public TestExpiration(Long absoluteExpiration, TimeUnit unit) {
            super(absoluteExpiration, unit);
        }

        @Override
        public Long getMillis() {
            return TimeUnit.MILLISECONDS.convert(expiresIn, unit);
        }
    }

    @Test
    public void testExpirationEncoder() throws Exception {
        ExpirationEncoder encoder = new ExpirationEncoder();
        EphemeralInput input = new EphemeralInput("foo", "bar");
        input.setExpiration(new TestExpiration(1L, TimeUnit.SECONDS));
        String key = encoder.encodeKey(input, new TestLocation());
        assertEquals("foo", key);
        String value = encoder.encodeValue(input, new TestLocation());
        String[] split = value.split("\\|");
        assertEquals(2, split.length);
        assertEquals("bar", split[0]);
        long encodedExpiry = Long.valueOf(split[1]);
        assertEquals(1000L, encodedExpiry);
    }
}
