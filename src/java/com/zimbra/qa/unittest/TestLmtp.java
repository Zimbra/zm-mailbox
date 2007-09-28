/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;

public class TestLmtp
extends TestCase {
    
    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestLmtp.class.getSimpleName();
    
    private ZMailbox mMbox;
    private Account mAccount;
    private String mOriginalWarnInterval;
    private int mOriginalWarnPercent;
    
    public void setUp()
    throws Exception {
        mMbox = TestUtil.getZMailbox("user1");
        mAccount = TestUtil.getAccount("user1");
        mOriginalWarnInterval = mAccount.getAttr(Provisioning.A_zimbraQuotaWarnInterval);
        mOriginalWarnPercent = mAccount.getIntAttr(Provisioning.A_zimbraQuotaWarnPercent, 0);
        cleanUp();
    }
    
    public void testQuotaWarning()
    throws Exception {
        // Initialize
        String address = TestUtil.getAddress(USER_NAME);
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaLastWarnTime, "");
        Provisioning.getInstance().modifyAttrs(mAccount, attrs);
        
        // Make sure there are no warnings already in the mailbox
        validateNumWarnings(0);
        
        // Make sure we haven't already hit the quota warning level
        TestUtil.insertMessageLmtp(1, NAME_PREFIX + " 1", address, address);
        validateNumWarnings(0);
        
        // Make sure setting quota warning to 0 is a no-op
        setQuotaWarnPercent(0);
        TestUtil.insertMessageLmtp(2, NAME_PREFIX + " 2", address, address);
        validateNumWarnings(0);
        
        // Make sure setting quota warning to 99 doesn't trigger the warning
        setQuotaWarnPercent(0);
        TestUtil.insertMessageLmtp(3, NAME_PREFIX + " 3", address, address);
        validateNumWarnings(0);
        
        // Make sure setting quota warning to 1 triggers the warning
        setQuotaWarnPercent(1);
        TestUtil.insertMessageLmtp(4, NAME_PREFIX + " 4", address, address);
        validateNumWarnings(1);
        
        // Make sure a second warning doesn't get sent (interval not exceeded)
        TestUtil.insertMessageLmtp(5, NAME_PREFIX + " 5", address, address);
        validateNumWarnings(1);
        
        // Make sure that a warning is triggered when the interval is exceeded
        setQuotaWarnInterval("1s");
        Thread.sleep(1000);
        TestUtil.insertMessageLmtp(6, NAME_PREFIX + " 6", address, address);
        validateNumWarnings(2);
        
        // Make sure that a second warning is not triggered when the interval is not set
        // (default: 1 day)
        setQuotaWarnInterval("");
        TestUtil.insertMessageLmtp(7, NAME_PREFIX + " 7", address, address);
        validateNumWarnings(2);
    }
    
    private void validateNumWarnings(int numWarnings)
    throws Exception {
        List<ZMessage> messages = TestUtil.search(mMbox, "Quota warning");
        assertEquals("Number of quota warnings", numWarnings, messages.size());
    }
    
    private void setQuotaWarnPercent(int percent)
    throws Exception {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaWarnPercent, Integer.toString(percent));
        Provisioning.getInstance().modifyAttrs(mAccount, attrs);
    }
    
    private void setQuotaWarnInterval(String interval)
    throws Exception {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraQuotaWarnInterval, interval);
        Provisioning.getInstance().modifyAttrs(mAccount, attrs);
    }
    
    public void tearDown()
    throws Exception {
        setQuotaWarnPercent(mOriginalWarnPercent);
        setQuotaWarnInterval(mOriginalWarnInterval);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(USER_NAME, "Quota warning");
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestLmtp.class));
    }
}
