package com.zimbra.qa.unittest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.mail.internet.MimeMessage;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.solr.SolrUtils;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;

public class TestSolrHeaderSearch {
    private static final String USER_NAME = TestSolrHeaderSearch.class.getSimpleName();
    private Account acct;
    private Mailbox mbox;
    private static final String INDEX_NAME = USER_NAME + "_index";

    @Before
    public void setUp() throws Exception {
        cleanUp();
        acct = TestUtil.createAccount(USER_NAME);
        acct.setMailboxIndexName(INDEX_NAME);
        mbox = TestUtil.getMailbox(USER_NAME);
        TestUtil.addMessage(mbox, generateMessage()).getId();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        try {
            String indexUrl = Provisioning.getInstance().getConfig().getIndexURL();
            if (indexUrl.startsWith("solrcloud")) {
                String zkHost = indexUrl.substring("solrcloud:".length());
                CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
                SolrUtils.deleteCloudIndex(client, INDEX_NAME);
            } else if (indexUrl.startsWith("solr")){
                String solrBaseUrl = indexUrl.substring("solr:".length());
                CloseableHttpClient httpClient = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
                SolrClient client = SolrUtils.getSolrClient(httpClient, solrBaseUrl, INDEX_NAME);
                SolrUtils.deleteStandaloneIndex(client, solrBaseUrl, INDEX_NAME);
            }
        } catch (Exception e) {
        }
    }

    private void search(String query, boolean shouldReturn) throws Exception {
        try (ZimbraQueryResults results = mbox.index.search(
                    new OperationContext(mbox), query, Collections.singleton(Type.MESSAGE), SortBy.DATE_DESC, 100)) {
            if (shouldReturn) {
                assertTrue("should see result for " + query, results.hasNext());
            } else {
                assertFalse("should not see result for " + query, results.hasNext());
            }
        }
    }

    private ParsedMessage generateMessage() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "test@zimbra.com");
        mm.setHeader("To", acct.getName());
        mm.setHeader("X-TestHeader", "100");
        mm.setHeader("Subject", "test");
        mm.setText("test");
        return new ParsedMessage(mm, false);
    }

    @Test
    public void testSearchHeader() throws Exception {
        search("#X-TestHeader:100", true);
        search("#X-TestHeader:101", false);
        search("#X-OtherHeader:100", false);

        //wildcards
        search("#X-TestHeader:*", true);
        search("#X-TestHeader:1*", true);
        search("#X-TestHeader:10*", true);
        search("#X-TestHeader:100*", true);
        search("#X-TestHeader:2*", false);

        //range queries
        search("#X-TestHeader:<=100", true);
        search("#X-TestHeader:<200", true);
        search("#X-TestHeader:<100", false);
        search("#X-TestHeader:>50", true);
        search("#X-TestHeader:>=100", true);
        search("#X-TestHeader:>100", false);

    }
}
