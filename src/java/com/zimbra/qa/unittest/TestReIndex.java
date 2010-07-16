/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexInfo;

/**
 * Unit test for ReIndex admin operation.
 * <p>
 * This test requires a Zimbra dev server instance.
 *
 * TODO: Add this class to {@link ZimbraSuite} once it supports JUnit 4
 * annotations.
 *
 * @author ysasaki
 */
public class TestReIndex {

    @BeforeClass
    public static void init() throws Exception {
        TestUtil.cliSetup();
    }

    @Test
    public void statusIdle() throws Exception {
        Account account = TestUtil.getAccount("user1");
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        Assert.assertEquals("idle", info.getStatus());
    }

}
