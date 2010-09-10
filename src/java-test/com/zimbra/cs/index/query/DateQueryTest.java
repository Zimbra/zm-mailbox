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
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit test for {@link DateQuery}.
 *
 * @author ysasaki
 */
public class DateQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.setInstance(new MockProvisioning());
        Provisioning.getInstance().createAccount("zero@zimbra.com", "secret",
                Collections.singletonMap(Provisioning.A_zimbraId, (Object) "0"));
    }

    @Test
    public void parseDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String expected = "Q(DATE,DATE,20100123000000)";

        query.parseDate("1/23/2010", tz, Locale.ENGLISH);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("23/1/2010", tz, Locale.FRENCH);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("23.1.2010", tz, Locale.GERMAN);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("23/1/2010", tz, Locale.ITALIAN);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("2010/1/23", tz, Locale.JAPANESE);
        Assert.assertEquals(expected, query.toString());

        query.parseDate("2010. 1. 23", tz, Locale.KOREAN);
        Assert.assertEquals(expected, query.toString());
    }

    @Test
    public void parseDateFallback() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        query.parseDate("1/23/2010", TimeZone.getTimeZone("UTC"), Locale.GERMAN);
        Assert.assertEquals("Q(DATE,DATE,20100123000000)",
                query.toString());
    }

}
