package com.zimbra.cs.datasource;

import junit.framework.TestCase;
import org.junit.Test;

public class SyncUtilTest extends TestCase {

    @Test
    public void testSendReportEmail() {
     assertEquals(true ,SyncUtil.sendReportEmail("test", "test", new String[]{"admin@platform-dev-abhishek.zimbradev.com"}));
    }
}