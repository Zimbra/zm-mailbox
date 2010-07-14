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

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Standalone unit test for {@link ZimbraQuery}.
 *
 * @author ysasaki
 */
public class TestZimbraQuery {

    @Test
    public void emptySubject() throws Exception {
        ZimbraQuery.BaseQuery query = ZimbraQuery.SubjectQuery.create(
                new TestMailbox(), ZimbraAnalyzer.getDefaultAnalyzer(),
                0, 0, "");
        Assert.assertEquals(ZimbraQuery.TextQuery.class, query.getClass());
        Assert.assertEquals("Q(UNKNOWN:(0),)", query.toString());
    }

    private static class TestMailbox extends Mailbox {

        protected TestMailbox() {
            super(new Mailbox.MailboxData());
        }

    }

}
