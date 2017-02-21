/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
