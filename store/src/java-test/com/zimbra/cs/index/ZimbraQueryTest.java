/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index;

import java.util.Collections;
import org.junit.Ignore;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.IOUtil;

/**
 * Unit test for {@link ZimbraQuery}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class ZimbraQueryTest {

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
    public void checkSortCompatibility() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        SearchParams params = new SearchParams();
        params.setQueryString("in:inbox content:test");

        params.setSortBy(SortBy.RCPT_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
            Assert.fail();
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }

        params.setSortBy(SortBy.ATTACHMENT_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        } catch (ServiceException e) {
            Assert.fail("Sorting by whether has attachments should be supported");
        }

        params.setSortBy(SortBy.FLAG_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        } catch (ServiceException e) {
            Assert.fail("Sorting by flagged should be supported");
        }

        params.setSortBy(SortBy.PRIORITY_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        } catch (ServiceException e) {
            Assert.fail("Sorting by priority should be supported");
        }
    }

    @Test
    public void testSanitizedQuery() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        SearchParams params = new SearchParams();
        params.setSortBy(SortBy.NONE);
        params.setQueryString("in:inbox content:test");
        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        Assert.assertEquals("Q(IN:$FOLDER)Q(&&)Q(l.content:$TEXT)", query.toSanitizedtring());

        params.setQueryString("in:inbox");
        query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        Assert.assertEquals("Q(IN:$FOLDER)", query.toSanitizedtring());

        params.setQueryString("conv:\"-554\" (underid:1 AND NOT underid:3 AND NOT underid:4)");
        query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        Assert.assertEquals("Q(ITEMID,$TEXT)Q(&&)Q(Q(UNDER:ANY_FOLDER)Q(&&)-Q(UNDER:$FOLDER)Q(&&)-Q(UNDER:$FOLDER))", query.toSanitizedtring());

        params.setQueryString("inid:15");
        query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        Assert.assertEquals("Q(IN:$FOLDER)", query.toSanitizedtring());
    }

    @Test
    public void searchResultMode() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        SearchParams params = new SearchParams();
        params.setQueryString("contact:test");
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.CONTACT));
        params.setFetchMode(SearchParams.Fetch.IDS);

        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        ZimbraQueryResults result = query.execute();
        Assert.assertTrue(result.hasNext());
        Assert.assertEquals(contact.getId(), result.getNext().getItemId());
        IOUtil.closeQuietly(result);
    }

    @Test
    public void calItemExpandRange() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        SearchParams params = new SearchParams();
        params.setQueryString("test");
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.APPOINTMENT));
        params.setFetchMode(SearchParams.Fetch.IDS);
        params.setCalItemExpandStart(1000);
        params.setCalItemExpandEnd(2000);

        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        // CalItemExpand range shouldn't be expanded yet
        Assert.assertEquals("ZQ: Q(l.content:test)", query.toString());
        // The order of HashSet iteration may be different on different platforms.
        Assert.assertTrue(query.toQueryString().matches(
                "\\(\\(content:test\\) AND -ID:/(Junk|Trash) -ID:/(Junk|Trash) \\)"));
    }

    @Test
    public void mdate() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DbConnection conn = DbPool.getConnection();
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, folder_id, type, flags, date, change_date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, ?, ?, 0, 0, 0, 0)", mbox.getId(), 101, Mailbox.ID_FOLDER_INBOX,
                MailItem.Type.MESSAGE.toByte(), 100, 1000);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, folder_id, type, flags, date, change_date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, ?, ?, 0, 0, 0, 0)", mbox.getId(), 102, Mailbox.ID_FOLDER_INBOX,
                MailItem.Type.MESSAGE.toByte(), 200, 2000);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, folder_id, type, flags, date, change_date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, ?, ?, 0, 0, 0, 0)", mbox.getId(), 103, Mailbox.ID_FOLDER_INBOX,
                MailItem.Type.MESSAGE.toByte(), 300, 3000);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, folder_id, type, flags, date, change_date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, ?, ?, 0, 0, 0, 0)", mbox.getId(), 104, Mailbox.ID_FOLDER_INBOX,
                MailItem.Type.MESSAGE.toByte(), 400, 4000);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, folder_id, type, flags, date, change_date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, ?, ?, 0, 0, 0, 0)", mbox.getId(), 105, Mailbox.ID_FOLDER_INBOX,
                MailItem.Type.MESSAGE.toByte(), 500, 5000);
        conn.commit();
        conn.closeQuietly();

        SearchParams params = new SearchParams();
        params.setQueryString("mdate:>3000000");
        params.setSortBy(SortBy.DATE_ASC);
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setFetchMode(SearchParams.Fetch.IDS);

        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        Assert.assertEquals("ZQ: Q(DATE:MDATE,197001010050-196912312359)", query.toString());
        ZimbraQueryResults result = query.execute();
        Assert.assertEquals(104, result.getNext().getItemId());
        Assert.assertEquals(105, result.getNext().getItemId());
        Assert.assertEquals(null, result.getNext());
        IOUtil.closeQuietly(result);
    }

    @Test
    public void quick() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Contact contact = mbox.createContact(null, new ParsedContact(Collections.singletonMap(
                ContactConstants.A_email, "test1@zimbra.com")), Mailbox.ID_FOLDER_CONTACTS, null);

        mbox.createContact(null, new ParsedContact(Collections.singletonMap(
                ContactConstants.A_email, "test2@zimbra.com")), Mailbox.ID_FOLDER_CONTACTS, null);

        SearchParams params = new SearchParams();
        params.setQueryString("test");
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.CONTACT));
        params.setQuick(true);

        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        ZimbraQueryResults result = query.execute();
        Assert.assertTrue("Expected at least 1 result", result.hasNext());
        Assert.assertEquals("Result item ID not as expected", contact.getId(), result.getNext().getItemId());
        Assert.assertFalse("More hits than expected", result.hasNext());
        IOUtil.closeQuietly(result);
    }

    @Test
    public void suggest() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, new ParsedMessage("Subject: all hands meeting".getBytes(), false),
                dopt, null);

        SearchParams params = new SearchParams();
        params.setQueryString("all hands me");
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setQuick(true);

        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        ZimbraQueryResults result = query.execute();
        Assert.assertEquals(msg.getId(), result.getNext().getItemId());
        IOUtil.closeQuietly(result);
    }

    @Test
    public void dumpster() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        SearchParams params = new SearchParams();
        params.setQueryString("test");
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setInDumpster(true);

        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        Assert.assertTrue(query.toQueryString(),
                query.toQueryString().matches("\\(\\(content:test\\) AND MDATE:\\(>\\d+\\) \\)"));
    }

}
