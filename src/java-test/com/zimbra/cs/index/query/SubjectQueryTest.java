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
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MockMailboxManager;

/**
 * Unit test for {@link SubjectQuery}.
 *
 * @author ysasaki
 */
public class SubjectQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.setInstance(new MockProvisioning());
        Provisioning.getInstance().createAccount("zero@zimbra.com", "secret",
                Collections.singletonMap(Provisioning.A_zimbraId, (Object) "0"));
    }

    @Test
    public void emptySubject() throws Exception {
        Mailbox mbox = new MockMailboxManager().getMailboxByAccountId("0");
        Query query = SubjectQuery.create(mbox,
                ZimbraAnalyzer.getDefaultAnalyzer(), "");
        Assert.assertEquals(TextQuery.class, query.getClass());
        Assert.assertEquals("Q(subject)", query.toString());
    }

}
