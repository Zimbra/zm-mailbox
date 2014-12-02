package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.mail.MessagingException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.service.ServiceException;

public class TestSubjectQueryOperators {
    private static String USER_NAME = "subjectquerytest";
    private static ZMailbox mbox;
    private static List<String> ids = new ArrayList<String>();

    @BeforeClass
    public static void setUp() throws ServiceException, IOException, MessagingException {
        TestUtil.createAccount(USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        ids.add(TestUtil.addMessage(mbox, "ab cd"));
        ids.add(TestUtil.addMessage(mbox, "ef gh"));
        ids.add(TestUtil.addMessage(mbox, "ij kl"));
        ids.add(TestUtil.addMessage(mbox, ">ef gh"));
        ids.add(TestUtil.addMessage(mbox, "<ef gh"));
        ids.add(TestUtil.addMessage(mbox, ">=ef gh"));
        ids.add(TestUtil.addMessage(mbox, "<=ef gh"));
    }

    @AfterClass
    public static void tearDown() throws ServiceException {
        TestUtil.deleteAccount(USER_NAME);
    }

    private void test(String query, List<String> expected) throws ServiceException {
        List<ZMessage> resp = TestUtil.search(mbox, query);
        assertEquals(expected.size(), resp.size());
        for (ZMessage msg: resp) {
            assertTrue(expected.contains(msg.getId()));
        }
    }

    @Test
    public void testSingleTermNoRange() throws ServiceException {
        test("subject:ab", ids.subList(0, 1));
    }

    @Test
    public void testSingleTermWithRangeLiterals() throws ServiceException {
        test("subject:\">ab\"", Collections.EMPTY_LIST);
        test("subject:\"<ab\"", Collections.EMPTY_LIST);
    }

    @Test
    public void testSingleTermRange() throws ServiceException {
        String[] expected = new  String[] {ids.get(0), ids.get(1), ids.get(2)};
        //the following two queries match "ab cd", "ef gh", "ij kl"
        test("subject:>ab", Arrays.asList(expected));
        test("subject:>=ab", Arrays.asList(expected));
        //this matches everything
        test("subject:<xyz", ids);
    }

    @Test
    public void testMultiTermNoRange() throws ServiceException {
        test("subject:\"ij kl\"", ids.subList(2, 3)); //only match "ij kl"
        /* The behavior is a bit weird here because the "=" character is ignored by SOLR,
         * meaning that ">ef gh" tokenizes to {">ef", "gh"}, while ">=ef gh" tokenizes to {"ef", "gh"}.
         * The following query matches "ef gh", "<=ef gh", ">=ef gh"
         */
        test("subject:\"ef gh\"", Arrays.asList(new String[] {ids.get(1), ids.get(5), ids.get(6)}));
    }

    @Test
    public void testMultiTermWithRangeLiterals() throws ServiceException {
        test("subject:\">ab cd\"", Collections.EMPTY_LIST);
        //see comment above about why the following query matches the first message
        test("subject:\">=ab cd\"", ids.subList(0, 1)); //only match "ab cd"
        test("subject:\">ef gh\"", ids.subList(3, 4)); //only match ">ef gh"
        test("subject:\"<ef gh\"", ids.subList(4, 5)); //only match "<ef gh"
    }

    @Test
    public void testMultiTermRange() throws ServiceException {
        test("subject:>=\"ab cd\"", ids.subList(0, 3)); //match "ab cd", "ef gh", "ij kl"
        test("subject:<\"ab cd\"", ids.subList(3, 7)); //match all subjects starting with > or <
    }

    @Test
    public void testBug78201() throws ServiceException, IOException, MessagingException {
        String id = TestUtil.addMessage(mbox, ">-+ nothing");
        test("subject:\">-+ nothing\"", Arrays.asList(new String[] {id}));
        mbox.deleteMessage(id);
    }
}
