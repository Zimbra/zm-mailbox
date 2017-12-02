/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Closeables;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;

public class TestSearchSortByRelevance {
    private static final String USER_NAME = TestSearchSortByDate.class.getSimpleName();
    private Account acct;
    private Mailbox mbox;

    @Before
    public void setUp() throws Exception {
        cleanUp();
        acct = TestUtil.createAccount(USER_NAME);
        mbox = TestUtil.getMailbox(USER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
    }

    private ZimbraQueryResults searchByRelevance(String query, boolean asc) throws ServiceException {
        return mbox.index.search(new OperationContext(mbox), query, Collections.singleton(Type.MESSAGE),
                asc ? SortBy.RELEVANCE_ASC : SortBy.RELEVANCE_DESC, 100);
    }

    private ParsedMessage generateMessage(String subject, String content) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "test@zimbra.com");
        mm.setHeader("To", acct.getName());
        mm.setHeader("Subject", subject);
        mm.setText(content);
        return new ParsedMessage(mm, false);
    }

    private void testSearchMultiTermQuery(boolean asc) throws Exception {
        //both terms in subject, with phrase match
        int id1 = TestUtil.addMessage(mbox, generateMessage("foo bar", "some content")).getId();
        //both terms in content, with phrase match
        int id2 = TestUtil.addMessage(mbox, generateMessage("some subject", "foo bar")).getId();
        //one term in subject, one in content
        int id3 = TestUtil.addMessage(mbox, generateMessage("foo subject", "bar content")).getId();
        //both term in subject
        int id4 = TestUtil.addMessage(mbox, generateMessage("foo test bar", "bar content")).getId();
        //both terms in content
        int id5 = TestUtil.addMessage(mbox, generateMessage("some subject", "foo test bar")).getId();

        //this is the expected relevance order given the dismax query currently used for searching the index.
        List<Integer> expected = new ArrayList<>(5);
        expected.add(id1); //subject phrase match
        expected.add(id4); //subject non-phrase match
        expected.add(id2); //content phrase match
        expected.add(id3); //mixed subject/content match
        expected.add(id5); //content non-phrase match

        if (asc) {
            Collections.reverse(expected);
        }

        ZimbraQueryResults results = searchByRelevance("foo bar", asc);
        int resultNum = 0;
        while (results.hasNext()) {
            ZimbraHit hit = results.getNext();
            assertEquals("wrong result", expected.get(resultNum), (Integer)hit.getItemId());
            resultNum++;
        }
        Closeables.closeQuietly(results);
    }

    @Test
    public void testSearchMultiTermQueryDesc() throws Exception {
        testSearchMultiTermQuery(false);
    }

    @Test
    public void testSearchMultiTermQueryAsc() throws Exception {
        testSearchMultiTermQuery(true);
    }

    @Test
    public void testSearchNoIndexClause() throws Exception {
        try {
            searchByRelevance("in:inbox", false);
            fail("should not be able to search by relevance without a text clause");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("can't be used without a text query"));
        }
        try {
            searchByRelevance("in:inbox or foo", false);
            fail("should not be able to search by relevance if any clause doesn't contain a text term");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("can't be used without a text query"));
        }
    }
}
