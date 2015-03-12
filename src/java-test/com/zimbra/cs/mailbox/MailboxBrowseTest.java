package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.ProvisioningUtil;

public class MailboxBrowseTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret",
                new HashMap<String, Object>());
    }

    int defaultCacheSizeSetting = 1024;

    @Before
    public void setUp() throws Exception {
        defaultCacheSizeSetting =  Provisioning.getInstance().getLocalServer().getIndexTermsCacheSize();
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        Provisioning.getInstance().getLocalServer().setIndexTermsCacheSize(defaultCacheSizeSetting);
    }

    @Test
    public void browse() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);

        DeliveryOptions dopt = new DeliveryOptions()
                .setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-2@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-3@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-4@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test2-1@sub2.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test2-2@sub2.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test2-3@sub2.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test3-1@sub3.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test3-2@sub3.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test4-1@sub4.zimbra.com".getBytes(),
                        false), dopt, null);
        List<BrowseTerm> terms = mbox.browse(null, Mailbox.BrowseBy.domains,
                null, 100);

        if(terms.size() != 4) {
            //we've got some garbage in indexed terms, lets print it out for investigation
           for(BrowseTerm term : terms) {
               ZimbraLog.test.error(String.format("found term: %s", term.getText()));
           }
        }

        Assert.assertEquals("Number of expected terms", 4, terms.size());
        Assert.assertEquals("sub1.zimbra.com", terms.get(0).getText());
        Assert.assertEquals("sub2.zimbra.com", terms.get(1).getText());
        Assert.assertEquals("sub3.zimbra.com", terms.get(2).getText());
        Assert.assertEquals("sub4.zimbra.com", terms.get(3).getText());
        Assert.assertEquals(8, terms.get(0).getFreq());
        Assert.assertEquals(6, terms.get(1).getFreq());
        Assert.assertEquals(4, terms.get(2).getFreq());
        Assert.assertEquals(2, terms.get(3).getFreq());
    }

    @Test
    public void browseWithSmallLimit() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions()
                .setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-2@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-3@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test1-4@sub1.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test2-1@sub2.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test2-2@sub2.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test2-3@sub2.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test3-1@sub3.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test3-2@sub3.zimbra.com".getBytes(),
                        false), dopt, null);
        mbox.addMessage(null,
                new ParsedMessage("From: test4-1@sub4.zimbra.com".getBytes(),
                        false), dopt, null);
        int defaultLimit = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexTermsCacheSize, 1024);
        Provisioning.getInstance().getLocalServer().setIndexTermsCacheSize(5);
        List<BrowseTerm> terms = mbox.browse(null, Mailbox.BrowseBy.domains,
                null, 100);
        Provisioning.getInstance().getLocalServer().setIndexTermsCacheSize(defaultLimit);
        if(terms.size() != 4) {
            //we've got some garbage in indexed terms, lets print it out for investigation
           for(BrowseTerm term : terms) {
               ZimbraLog.test.error(String.format("found term: %s", term.getText()));
           }
        }
        Assert.assertEquals("Number of expected terms", 4, terms.size());
        Assert.assertEquals("sub1.zimbra.com", terms.get(0).getText());
        Assert.assertEquals("sub2.zimbra.com", terms.get(1).getText());
        Assert.assertEquals("sub3.zimbra.com", terms.get(2).getText());
        Assert.assertEquals("sub4.zimbra.com", terms.get(3).getText());
        Assert.assertEquals(8, terms.get(0).getFreq());
        Assert.assertEquals(6, terms.get(1).getFreq());
        Assert.assertEquals(4, terms.get(2).getFreq());
        Assert.assertEquals(2, terms.get(3).getFreq());
    }

    @Test
    public void browseOverLimit() throws Exception {
        Provisioning.getInstance().getLocalServer().setIndexTermsCacheSize(256);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        int indexTermsCacheSize = Provisioning.getInstance().getLocalServer().getIndexTermsCacheSize();
        int numDomains = indexTermsCacheSize
                + indexTermsCacheSize / 3;
        DeliveryOptions dopt = new DeliveryOptions()
                .setFolderId(Mailbox.ID_FOLDER_INBOX);
        for (int i = 0; i < numDomains; i++) {
            mbox.addMessage(
                    null,
                    new ParsedMessage(String.format(
                            "From: test1-1@sub%d.zimbra.com", i).getBytes(),
                            false), dopt, null);
            if (i % 2 == 0) {
                mbox.addMessage(
                        null,
                        new ParsedMessage(
                                String.format("From: test1-2@sub%d.zimbra.com",
                                        i).getBytes(), false), dopt, null);
            }
            if (i % 3 == 0) {
                mbox.addMessage(
                        null,
                        new ParsedMessage(
                                String.format("From: test1-3@sub%d.zimbra.com",
                                        i).getBytes(), false), dopt, null);
            }
        }
        List<BrowseTerm> terms = mbox.browse(null, Mailbox.BrowseBy.domains,
                null, 100);
        Assert.assertEquals("Number of expected terms", 100, terms.size());

        terms = mbox.browse(null, Mailbox.BrowseBy.domains, null,
                numDomains * 2);

        if(terms.size() != numDomains) {
            //we've got some garbage in indexed domains, lets print it out for investigation
           for(BrowseTerm term : terms) {
               ZimbraLog.test.error(String.format("found term: %s", term.getText()));
           }
        }
        Assert.assertEquals("Number of expected terms", numDomains,
                terms.size());
    }

}
