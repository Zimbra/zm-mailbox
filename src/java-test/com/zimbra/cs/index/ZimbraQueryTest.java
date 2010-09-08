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

package com.zimbra.cs.index;

import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.query.parser.QueryParserException;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MockMailboxManager;
import com.zimbra.cs.service.util.ItemId;

/**
 * Unit test for {@link ZimbraQuery}.
 *
 * @author ysasaki
 */
public class ZimbraQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.setInstance(new MockProvisioning());
        Provisioning.getInstance().createAccount("zero@zimbra.com", "secret",
                Collections.singletonMap(Provisioning.A_zimbraId, (Object) "0"));
    }

    @Test
    public void emptySubject() throws Exception {
        Mailbox mbox = new MockMailboxManager().getMailboxByAccountId("0");
        ZimbraQuery.BaseQuery query = ZimbraQuery.SubjectQuery.create(
                mbox, ZimbraAnalyzer.getDefaultAnalyzer(),
                0, 0, "");
        Assert.assertEquals(ZimbraQuery.TextQuery.class, query.getClass());
        Assert.assertEquals("Q(UNKNOWN:(0))", query.toString());
    }

    @Test
    public void parseDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ZimbraQuery.DateQuery query = new ZimbraQuery.DateQuery(QueryParser.DATE);
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
        ZimbraQuery.DateQuery query = new ZimbraQuery.DateQuery(QueryParser.DATE);
        query.parseDate("1/23/2010", TimeZone.getTimeZone("UTC"), Locale.GERMAN);
        Assert.assertEquals("Q(DATE,DATE,20100123000000)",
                query.toString());
    }

    @Test
    public void parseSize() throws Exception {
        ZimbraQuery.SizeQuery query = new ZimbraQuery.SizeQuery(0, 0, "1KB");
        Assert.assertEquals("Q(UNKNOWN:(0),1024)", query.toString());

        query = new ZimbraQuery.SizeQuery(0, 0, ">1KB");
        Assert.assertEquals("Q(BIGGER,1024)", query.toString());

        query = new ZimbraQuery.SizeQuery(0, 0, "<1KB");
        Assert.assertEquals("Q(SMALLER,1024)", query.toString());

        query = new ZimbraQuery.SizeQuery(0, 0, ">=1KB");
        Assert.assertEquals("Q(BIGGER,1023)", query.toString());

        query = new ZimbraQuery.SizeQuery(0, 0, "<=1KB");
        Assert.assertEquals("Q(SMALLER,1025)", query.toString());

        query = new ZimbraQuery.SizeQuery(0, 0, "1 KB");
        Assert.assertEquals("Q(UNKNOWN:(0),1024)", query.toString());

        try {
            query = new ZimbraQuery.SizeQuery(0, 0, "x KB");
            Assert.fail();
        } catch (QueryParserException expected) {
        }
    }

    @Test
    public void inAnyFolder() throws Exception {
        Mailbox mbox = new MockMailboxManager().getMailboxByAccountId("0");

        ZimbraQuery.BaseQuery query = ZimbraQuery.InQuery.Create(mbox, 0,
                new ItemId("0", 1), null, true);
        Assert.assertEquals("Q(IN,UNDER:ANY_FOLDER)", query.toString());

        query = ZimbraQuery.InQuery.Create(mbox, 0, new ItemId("0", 1), null, false);
        Assert.assertEquals("Q(IN,IN:1)", query.toString());

        query = ZimbraQuery.InQuery.Create(mbox, 0, new ItemId("1", 1), null, true);
        Assert.assertEquals("Q(IN,UNDER:1:1)", query.toString());

        query = ZimbraQuery.InQuery.Create(mbox, 0, new ItemId("1", 1), null, false);
        Assert.assertEquals("Q(IN,IN:1:1)", query.toString());
    }

    @Test
    public void quoteFieldQuery() throws Exception {
        Mailbox mbox = new MockMailboxManager().getMailboxByAccountId("0");
        ZimbraQuery.BaseQuery query = ZimbraQuery.FieldQuery.Create(mbox,
                ZimbraAnalyzer.getDefaultAnalyzer(), 0, QueryParser.FIELD,
                "#company", "zimbra:vmware");
        Assert.assertEquals("#company:\"zimbra:vmware\"",
                query.toQueryString("company:zimbra:vmware"));
        Assert.assertEquals("#company:\"zimbra@vmware\"",
                query.toQueryString("company:zimbra@vmware"));
        Assert.assertEquals("#company:\"zimbra\\\"vmware\"",
                query.toQueryString("company:zimbra\"vmware"));
    }

}
