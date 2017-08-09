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
package com.zimbra.cs.index.query;

import java.text.ParseException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link SizeQuery}.
 *
 * @author ysasaki
 */
public final class SizeQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MockProvisioning prov = new MockProvisioning();
        prov.createAccount("zero@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);
    }

    @Test
    public void parseSize() throws Exception {
        SizeQuery query = new SizeQuery(SizeQuery.Type.EQ, "1KB");
        Assert.assertEquals("Q(SIZE:=1024)", query.toString());

        query = new SizeQuery(SizeQuery.Type.EQ, ">1KB");
        Assert.assertEquals("Q(SIZE:>1024)", query.toString());

        query = new SizeQuery(SizeQuery.Type.EQ, "<1KB");
        Assert.assertEquals("Q(SIZE:<1024)", query.toString());

        query = new SizeQuery(SizeQuery.Type.EQ, ">=1KB");
        Assert.assertEquals("Q(SIZE:>1023)", query.toString());

        query = new SizeQuery(SizeQuery.Type.EQ, "<=1KB");
        Assert.assertEquals("Q(SIZE:<1025)", query.toString());

        query = new SizeQuery(SizeQuery.Type.EQ, "1 KB");
        Assert.assertEquals("Q(SIZE:=1024)", query.toString());

        try {
            query = new SizeQuery(SizeQuery.Type.EQ, "x KB");
            Assert.fail();
        } catch (ParseException expected) {
        }
    }

}
