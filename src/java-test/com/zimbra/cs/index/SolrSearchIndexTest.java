package com.zimbra.cs.index;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.solr.SolrIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedContact;

/**
 * Unit test for {@link SolrIndex}.
 */
@Ignore("Disabled as SolrIndex is experimental at this point.")
public class SolrSearchIndexTest extends AbstractIndexStoreTest {
    private static final CloudSolrClient solrServer = new CloudSolrClient("localhost:9983");
    @Override
    protected String getIndexStoreFactory() {
        return "com.zimbra.cs.index.solr.SolrIndex$Factory";
    }

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("log4j.configuration", "log4j-test.properties");
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        testAcct = prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        originalIndexStoreFactory = IndexStore.getFactory().getClass().getName();
    }

    /* @Override
    protected boolean indexStoreAvailable() {

        try {
            CoreAdminResponse resp = CoreAdminRequest.getStatus("collection1", solrServer);
            return true;
        } catch (SolrServerException e) {
           return false;
        } catch (IOException e) {
            return false;
        }
    }*/

    @Override
    protected void cleanupForIndexStore() {
        try {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set("action", CollectionAction.DELETE.toString());
            params.set("name", testAcct.getId());
            SolrRequest req = new QueryRequest(params);
            req.setPath("/admin/collections");
            req.process(solrServer);
            //TODO check for errors

        } catch (SolrServerException e) {
            ZimbraLog.index.error("Problem deleting Solr collection" , e);
        } catch (IOException e) {
            ZimbraLog.index.error("Problem deleting Solr collection" , e);
        }
    }

    @Override
    @Test
    public void termQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST termQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Contact contact = createContact(mbox, "First", "Last", "test@zimbra.com");
        createContact(mbox, "a", "bc", "abc@zimbra.com");
        createContact(mbox, "j", "k", "j.k@zimbra.com");
        createContact(mbox, "Matilda", "Higgs-Bozon", "matilda.higgs.bozon@zimbra.com");

        // Stick with just one IndexStore - the one cached in Mailbox:
        //    IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
       // Thread.sleep(5000);
        ZimbraTopDocs result = searcher.search(
                  new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "none@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for none@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'none@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for none@zimbra.com", 0, result.getTotalHits());

        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "test@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for test@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'test@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for test@zimbra.com", 1, result.getTotalHits());
        //Thread.sleep(5000);//allow time to update core stats
        Assert.assertEquals(4, searcher.getIndexReader().numDocs());
        searcher.close();
    }

    @Override
    @Test
    public void deleteDocument() throws Exception {
        ZimbraLog.test.debug("--->TEST deleteDocument");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        IndexStore index = mbox.index.getIndexStore();
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        Assert.assertEquals("maxDocs at start", 0, indexer.maxDocs());
        Contact contact1 = createContact(mbox, "James", "Peters", "test1@zimbra.com");
        createContact(mbox, "Emma", "Peters", "test2@zimbra.com");

        ZimbraIndexSearcher searcher = index.openSearcher();
        Assert.assertEquals("numDocs after 2 adds", 2, searcher.getIndexReader().numDocs());
        ZimbraTopDocs result = searcher.search(
        new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for '@zimbra.com'", result);
        ZimbraLog.test.debug("Result for search for '@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Total hits after 2 adds", 2, result.getTotalHits());
        searcher.close();

        indexer = index.openIndexer();
        indexer.deleteDocument(Collections.singletonList(contact1.getId()));
        indexer.close();

        searcher = index.openSearcher();
        Assert.assertEquals("numDocs after 2 adds/1 del", 1, searcher.getIndexReader().numDocs());
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object after 2 adds/1 del", result);
        ZimbraLog.test.debug("Result for search for '@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Total hits after 2 adds/1 del", 1, result.getTotalHits());
        searcher.close();
    }

    @Override
    @Test
    public void getCount() throws Exception {
        ZimbraLog.test.debug("--->TEST getCount");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        IndexStore index = mbox.index.getIndexStore();
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        Assert.assertEquals("maxDocs at start", 0, indexer.maxDocs());
        createContact(mbox, "Jane", "Peters", "test1@zimbra.com");
        createContact(mbox, "Emma", "Peters", "test2@zimbra.com");
        createContact(mbox, "Fiona", "Peters", "test3@zimbra.com");
        createContact(mbox, "Edward", "Peters", "test4@zimbra.com");
        Assert.assertEquals("maxDocs after adding 4 contacts", 4, indexer.maxDocs());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        Assert.assertEquals("numDocs after adding 4 contacts", 4, searcher.getIndexReader().numDocs());
        Assert.assertEquals("docs which match 'test1'", 1,
                searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "test1")));
        Assert.assertEquals("docs which match '@zimbra.com'", 4,
               searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")));
        searcher.close();
    }
    private Contact createContact(Mailbox mbox, String firstName, String lastName, String email)
            throws ServiceException {
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Map<String, Object> fields;
        fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, firstName,
                ContactConstants.A_lastName, lastName,
                ContactConstants.A_email, email);
        return mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
    }
}
