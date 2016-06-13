/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link ZimbraAuthToken}.
 *
 * @author ysasaki
 */
public class ZimbraAuthTokenTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("user1@example.zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void test() throws Exception {
        Account a = Provisioning.getInstance().get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        long start = System.currentTimeMillis();
        String encoded = at.getEncoded();
        for (int i = 0; i < 1000; i++) {
            new ZimbraAuthToken(encoded);
        }
        System.out.println("Encoded 1000 auth-tokens elapsed=" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ZimbraAuthToken.getAuthToken(encoded);
        }
        System.out.println("Decoded 1000 auth-tokens elapsed=" + (System.currentTimeMillis() - start));
    }
    
    @Test
    public void testEncodedDifferentOnTokenIDReset() throws Exception {
        Account a = Provisioning.getInstance().get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        ZimbraAuthToken clonedAuthToken = at.clone();
        clonedAuthToken.resetTokenId();
        Assert.assertFalse(at.getEncoded().equals(clonedAuthToken.getEncoded()));
    }

}
