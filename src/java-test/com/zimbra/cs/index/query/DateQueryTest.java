/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.text.ParseException;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
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
        LC.zimbra_class_provisioning.setDefault(MockProvisioning.class.getName());
        MockProvisioning prov = (MockProvisioning) Provisioning.getInstance();
        prov.createAccount("zero@zimbra.com", "secret",
                Collections.<String, Object> singletonMap(Provisioning.A_zimbraId, "0-0-0"));
    }

    @Test
    public void parseDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String expected = "Q(DATE,DATE,201001230000-201001240000)";

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
    public void parseInvalidDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        TimeZone tz = TimeZone.getTimeZone("UTC");

        try {
            query.parseDate("-1/1/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("1/-1/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("1/1/-2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("111/1/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }

        try {
            query.parseDate("1/111/2010", tz, Locale.ENGLISH);
            Assert.fail();
        } catch (ParseException expected) {
        }
    }

    @Test
    public void parseDateFallback() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateQuery query = new DateQuery(DateQuery.Type.DATE);
        query.parseDate("1/23/2010", TimeZone.getTimeZone("UTC"), Locale.GERMAN);
        Assert.assertEquals("Q(DATE,DATE,201001230000-201001240000)", query.toString());
    }

}
