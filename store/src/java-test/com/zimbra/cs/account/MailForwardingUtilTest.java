/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MailForwardingUtilTest {

    private Account account;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();

        Provisioning prov = Provisioning.getInstance();

        account = prov.createAccount(
                "user@test.com",
                "secret",
                new HashMap<String, Object>());
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test(expected = ServiceException.class)
    public void testSelfForwardingShouldThrowException() throws Exception {

        MailForwardingUtil.validateSelfForwarding(
                account,
                "user@test.com");
    }

    @Test(expected = ServiceException.class)
    public void testSelfForwardingCaseInsensitiveShouldThrowException()
            throws Exception {

        MailForwardingUtil.validateSelfForwarding(
                account,
                "USER@TEST.COM");
    }

    @Test
    public void testValidForwardingShouldNotThrowException()
            throws Exception {

        MailForwardingUtil.validateSelfForwarding(
                account,
                "other@test.com");
    }

    @Test
    public void testMultipleValidForwardingAddressesShouldNotThrowException()
            throws Exception {

        MailForwardingUtil.validateSelfForwarding(
                account,
                "other1@test.com,other2@test.com");
    }

    @Test(expected = ServiceException.class)
    public void testMultipleAddressesContainingSelfShouldThrowException()
            throws Exception {

        MailForwardingUtil.validateSelfForwarding(
                account,
                "other@test.com,user@test.com");
    }

    @Test
    public void testNullForwardingAddressShouldNotThrowException()
            throws Exception {

        MailForwardingUtil.validateSelfForwarding(
                account,
                null);
    }

    @Test
    public void testEmptyForwardingAddressShouldNotThrowException()
            throws Exception {

        MailForwardingUtil.validateSelfForwarding(
                account,
                "");
    }
}
