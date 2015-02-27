/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.IndexStore.Factory;
import com.zimbra.cs.index.solr.SolrCloudIndex;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.soap.admin.type.CacheEntryType;


public class TestIndex  {

    private static final String NAME_PREFIX = TestIndex.class.getSimpleName();
    private boolean originalLCSetting = false;
    private int mOriginalTextLimit;
    protected static final String BASE_DOMAIN_NAME = TestLdap.baseDomainName(TestSolrCloud.class);
    protected static final String USER_NAME = "TestSolrCloud-user1@" + BASE_DOMAIN_NAME;
    private Account acct = null;
    
    @Before
	public void setUp() throws Exception {
    	originalLCSetting = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexManualCommit, true);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
        mOriginalTextLimit = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit));
        cleanUp();
        TestUtil.createDomain(BASE_DOMAIN_NAME);
        acct = TestUtil.createAccount(USER_NAME);
    }
    
    @After
    public void tearDown() throws Exception {
        setTextLimit(mOriginalTextLimit);
        Provisioning.getInstance().getConfig().addDefaultAnalyzerStopWords("a");
        cleanUp();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(originalLCSetting);
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
        TestUtil.deleteDomain(BASE_DOMAIN_NAME);
    }

    @Test
    public void testDeleteIndex() throws Exception {
        Factory indexStoreFactory = IndexStore.getFactory();
        //create an account
        assertTrue("failed to create an account", TestUtil.accountExists(acct.getName()));
        TestUtil.addMessage(TestUtil.getMailbox(acct.getName()), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
        if(!(indexStoreFactory instanceof SolrCloudIndex.Factory)) {
            assertEquals("failed to find injected message", 1,TestUtil.search(TestUtil.getMailbox(acct.getName()), "chorus", MailItem.Type.MESSAGE).size());
        }
        IndexStore indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertTrue("failed to create an index", indexStore.indexExists());

        Mailbox mbox = TestUtil.getMailbox(acct.getName());
        mbox.index.deleteIndex();
        if(indexStoreFactory instanceof SolrCloudIndex.Factory) {
            Thread.sleep(5000); //let ZK update clusterstate.json
        }
        indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertFalse("failed to delete an index", indexStore.indexExists());
    }

    @Test
    public void testDeleteDeletedIndex() throws Exception {
        Factory indexStoreFactory = IndexStore.getFactory();

        //create an account
        assertTrue("failed to create an account", TestUtil.accountExists(acct.getName()));
        TestUtil.addMessage(TestUtil.getMailbox(acct.getName()), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
        if(!(indexStoreFactory instanceof SolrCloudIndex.Factory)) {
            assertEquals("failed to find injected message", 1,TestUtil.search(TestUtil.getMailbox(acct.getName()), "chorus", MailItem.Type.MESSAGE).size());
        }
        IndexStore indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertTrue("failed to create an index", indexStore.indexExists());

        Mailbox mbox = TestUtil.getMailbox(acct.getName());
        mbox.index.deleteIndex();
        if(indexStoreFactory instanceof SolrCloudIndex.Factory) {
            Thread.sleep(5000); //let ZK update clusterstate.json
        }
        indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertFalse("failed to delete an index", indexStore.indexExists());
        try {
            mbox.index.deleteIndex();
        } catch (Exception e) {
            fail("should not be getting an exception ");
        }
        
        indexStore = indexStoreFactory.getIndexStore(acct.getId());
        
        try {
            indexStore.deleteIndex();
        } catch (Exception e) {
            fail("should not be getting an exception ");
        }
        
        assertFalse("failed to delete an index", indexStore.indexExists());
    }
    
    @Test
    public void testRecoverLostIndex() throws Exception {
        Factory indexStoreFactory = IndexStore.getFactory();

        //create an account
        assertTrue("failed to create an account", TestUtil.accountExists(acct.getName()));
        TestUtil.addMessage(TestUtil.getMailbox(acct.getName()), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
        if(!(indexStoreFactory instanceof SolrCloudIndex.Factory)) {
            assertEquals("failed to find injected message", 1,TestUtil.search(TestUtil.getMailbox(acct.getName()), "chorus", MailItem.Type.MESSAGE).size());
        }
        IndexStore indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertTrue("failed to create an index", indexStore.indexExists());

        Mailbox mbox = TestUtil.getMailbox(acct.getName());
        mbox.index.deleteIndex();
        if(indexStoreFactory instanceof SolrCloudIndex.Factory) {
            Thread.sleep(5000); //let ZK update clusterstate.json
        }
        indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertFalse("failed to delete an index", indexStore.indexExists());
        
        TestUtil.addMessage(TestUtil.getMailbox(acct.getName()), "We found the enemy and he is us");
        if(!(indexStoreFactory instanceof SolrCloudIndex.Factory)) {
            assertEquals("failed to find injected message", 1,TestUtil.search(TestUtil.getMailbox(acct.getName()), "enemy", MailItem.Type.MESSAGE).size());
            assertEquals("Should not be finding message added before index was deleted", 0,TestUtil.search(TestUtil.getMailbox(acct.getName()), "chorus", MailItem.Type.MESSAGE).size());
        }
    }
    
    @Test
    public void testDeleteMailbox() throws Exception {
        Factory indexStoreFactory = IndexStore.getFactory();

        //create an account
        assertTrue("failed to create an account", TestUtil.accountExists(acct.getName()));
        TestUtil.addMessage(TestUtil.getMailbox(acct.getName()), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
        if(!(indexStoreFactory instanceof SolrCloudIndex.Factory)) {
            assertEquals("failed to find injected message", 1,TestUtil.search(TestUtil.getMailbox(acct.getName()), "chorus", MailItem.Type.MESSAGE).size());
        }
        IndexStore indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertTrue("failed to create an index", indexStore.indexExists());
        
        Mailbox mbox = TestUtil.getMailbox(acct.getName());
        mbox.deleteMailbox();
        if(indexStoreFactory instanceof SolrCloudIndex.Factory) {
            Thread.sleep(5000); //let ZK update clusterstate.json
        }
        
        indexStore = indexStoreFactory.getIndexStore(acct.getId());
        assertFalse("Index should not exist after mailbox " + acct.getId() + " is deleted", indexStore.indexExists());
        
        try {
            indexStore.deleteIndex();
        } catch (Exception e) {
            fail("should not be getting an exception ");
        }
        
        assertFalse("Index should not exist for account " + acct.getId(), indexStore.indexExists());
    }
    
    @Test
    public void testIndexedTextLimit() throws Exception {
        Assume.assumeTrue("com.zimbra.cs.index.solr.SolrIndex$Factory".equals(LC.zimbra_class_index_store_factory.value()));
        // Test text attachment
        StringBuilder body = new StringBuilder();
        for (int i = 1; i < 100; i++) {
            body.append("Walrus walrus walrus walrus walrus walrus walrus.\n");
        }
        body.append("Goo goo goo joob.\n");

        // Test text truncated
        setTextLimit(50);
        String subject = NAME_PREFIX + " text attachment 1";
        String msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", MimeConstants.CT_TEXT_PLAIN).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", null);

        // Test HTML truncated
        subject = NAME_PREFIX + " HTML attachment 1";
        String htmlBody = "<html>\n" + body + "</html>";
        msgId = sendMessage(subject, htmlBody.getBytes(), "attachment.html", MimeConstants.CT_TEXT_HTML).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", null);

        // Test text not truncated
        setTextLimit(100000);
        subject = NAME_PREFIX + " text attachment 2";
        msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", MimeConstants.CT_TEXT_PLAIN).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", msgId);

        // Test HTML not truncated
        subject = NAME_PREFIX + " HTML attachment 2";
        msgId = sendMessage(subject, htmlBody.getBytes(), "attachment.html", MimeConstants.CT_TEXT_HTML).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", msgId);

        // Test attached message subject truncated
        subject = NAME_PREFIX + " subject";
        String attachedMsg = TestUtil.getTestMessage("Pigs from a gun", "recipient", "sender", null);
        setTextLimit(4);
        msgId = sendMessage(subject, attachedMsg.getBytes(), "attachment.msg", MimeConstants.CT_MESSAGE_RFC822).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" pigs", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" gun", null);
    }


    /**
     * Verifies the fix to bug 54613.
     */
    @Test
    public void testFilenameSearch() throws Exception {
        Assume.assumeTrue("com.zimbra.cs.index.solr.SolrIndex$Factory".equals(LC.zimbra_class_index_store_factory.value()));
        ZMailbox mbox = TestUtil.getZMailbox(acct.getName());
        String filename = NAME_PREFIX + " testFilenameSearch.txt";
        TestUtil.createDocument(mbox, Integer.toString(Mailbox.ID_FOLDER_BRIEFCASE),
            filename, "text/plain", "This is the data for testFilenameSearch.".getBytes());
        assertEquals(0, TestUtil.search(mbox, "filename:Blob*", ZSearchParams.TYPE_DOCUMENT).size());
        assertEquals(1, TestUtil.search(mbox, "filename:\"" + filename + "\"", ZSearchParams.TYPE_DOCUMENT).size());
    }

    /**
     * Sends a message with the specified attachment, waits for the message to
     * arrives, and runs a query.
     * @param subject the subject of the message
     * @param attData attachment data
     * @param attName attachment name
     * @param attContentType attachment content type
     * @param query query to run after message arrives
     * @return <tt>true</tt> if the query returns the message
     */
    private ZMessage sendMessage(String subject, byte[] attData, String attName, String attContentType)
    throws Exception {
        Factory indexStoreFactory = IndexStore.getFactory();
        // Send message
        ZMailbox mbox = TestUtil.getZMailbox(acct.getName());
        String attachmentId = mbox.uploadAttachment(attName, attData, attContentType, 5000);
        TestUtil.sendMessage(mbox, acct.getName(), subject, "Cranberry sauce", attachmentId);
        if(indexStoreFactory instanceof SolrCloudIndex.Factory) {
            Thread.sleep(5000); //let ZK update clusterstate.json
        }
        String query = "in:inbox subject:\"" + subject + "\"";
        return TestUtil.waitForMessage(mbox, query);
    }

    private void checkQuery(String query, String msgId) throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(acct.getName());
        List<ZMessage> messages = TestUtil.search(mbox, query);
        if (msgId == null) {
            assertEquals(0, messages.size());
        } else {
            assertEquals(1, messages.size());
            assertEquals(msgId, messages.get(0).getId());
        }
    }

    private void setTextLimit(int numBytes)
    throws Exception {
    	Provisioning prov = Provisioning.getInstance();
    	prov.flushCache(CacheEntryType.all, null);
        TestUtil.setServerAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit, Integer.toString(numBytes));
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestIndex.class);
    }
}
