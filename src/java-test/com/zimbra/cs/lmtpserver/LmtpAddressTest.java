package com.zimbra.cs.lmtpserver;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class LmtpAddressTest {

    /**
     * Tests fix for bug 22712.
     */
    @Test
    public void parseQuotedLocalPart() {
        String recipient = "<\"test.\"@domain.com>";
        Assert.assertEquals("test.@domain.com", new LmtpAddress(recipient, null, null).getEmailAddress());
        recipient = "<\"\\\"test.\\\"\"@domain.com>";
        Assert.assertEquals("\"test.\"@domain.com", new LmtpAddress(recipient, null, null).getEmailAddress());
    }
}
