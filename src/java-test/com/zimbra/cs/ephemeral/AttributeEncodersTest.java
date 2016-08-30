package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.zimbra.common.util.Pair;
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
        Pair<String, String> keyValue = encoder.encode(input, new TestLocation());
        assertEquals("foo", keyValue.getFirst());
        String value = keyValue.getSecond();
        String[] split = value.split("\\|");
        assertEquals(2, split.length);
        assertEquals("bar", split[0]);
        long encodedExpiry = Long.valueOf(split[1]);
        assertEquals(1000L, encodedExpiry);
    }
}
