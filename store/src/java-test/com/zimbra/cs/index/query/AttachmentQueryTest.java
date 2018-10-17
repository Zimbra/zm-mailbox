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
package com.zimbra.cs.index.query;

import java.util.HashMap;
import org.junit.Ignore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link AttachmentQuery}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class AttachmentQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void attachQueryToQueryString() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Query query = AttachmentQuery.createQuery("any");
        Assert.assertEquals("(attachment:any)", ((LuceneQueryOperation) query.compile(mbox, true)).toQueryString());
    }

    @Test
    public void attachQueryNotAny() throws Exception {
        Query query = AttachmentQuery.createQuery("any");
        query.setModifier(Query.Modifier.MINUS);
        Assert.assertEquals("Q(attachment:none)", query.toString());
    }

    @Test
    public void attachQueryUnknown() throws Exception {
        Query query = AttachmentQuery.createQuery("unknowncontenttype");
        Assert.assertEquals("Q(attachment:unknowncontenttype)", query.toString());
    }

    @Test
    public void attachQueryMsWord() throws Exception {
        Query query = AttachmentQuery.createQuery("msword");
        Assert.assertEquals(
            "(Q(attachment:application/msword)" +
            " || Q(attachment:application/vnd.openxmlformats-officedocument.wordprocessingml.document)" +
            " || Q(attachment:application/vnd.openxmlformats-officedocument.wordprocessingml.template)" +
            " || Q(attachment:application/vnd.ms-word.document.macroenabled.12)" +
            " || Q(attachment:application/vnd.ms-word.template.macroenabled.12))",
            query.toString());
    }

    @Test
    public void typeQueryMsWord() throws Exception {
        Query query = TypeQuery.createQuery("msword");
        Assert.assertEquals(
            "(Q(type:application/msword)" +
            " || Q(type:application/vnd.openxmlformats-officedocument.wordprocessingml.document)" +
            " || Q(type:application/vnd.openxmlformats-officedocument.wordprocessingml.template)" +
            " || Q(type:application/vnd.ms-word.document.macroenabled.12)" +
            " || Q(type:application/vnd.ms-word.template.macroenabled.12))",
            query.toString());
    }

    @Test
    public void typeQueryToQueryString() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Query query = TypeQuery.createQuery("any");
        Assert.assertEquals("(type:any)", ((LuceneQueryOperation) query.compile(mbox, true)).toQueryString());
    }

}
