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
package com.zimbra.cs.index.query;

import java.util.Collections;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.query.parser.QueryParserException;

/**
 * Unit test for {@link SizeQuery}.
 *
 * @author ysasaki
 */
public class SizeQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.setInstance(new MockProvisioning());
        Provisioning.getInstance().createAccount("zero@zimbra.com", "secret",
                Collections.singletonMap(Provisioning.A_zimbraId, (Object) "0"));
    }

    @Test
    public void parseSize() throws Exception {
        SizeQuery query = new SizeQuery(0, "1KB");
        Assert.assertEquals("Q(UNKNOWN:(0),1024)", query.toString());

        query = new SizeQuery(0, ">1KB");
        Assert.assertEquals("Q(BIGGER,1024)", query.toString());

        query = new SizeQuery(0, "<1KB");
        Assert.assertEquals("Q(SMALLER,1024)", query.toString());

        query = new SizeQuery(0, ">=1KB");
        Assert.assertEquals("Q(BIGGER,1023)", query.toString());

        query = new SizeQuery(0, "<=1KB");
        Assert.assertEquals("Q(SMALLER,1025)", query.toString());

        query = new SizeQuery(0, "1 KB");
        Assert.assertEquals("Q(UNKNOWN:(0),1024)", query.toString());

        try {
            query = new SizeQuery(0, "x KB");
            Assert.fail();
        } catch (QueryParserException expected) {
        }
    }

}
