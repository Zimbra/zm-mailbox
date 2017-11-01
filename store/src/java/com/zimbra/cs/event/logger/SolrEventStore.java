package com.zimbra.cs.event.logger;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrUtils;

public class SolrEventStore extends EventStore {

    private SolrCloudHelper solrHelper;

    public SolrEventStore(String accountId, SolrCloudHelper solrHelper) {
        super(accountId);
        this.solrHelper = solrHelper;
    }

    @Override
    public void deleteEventsByAccount() throws ServiceException {
        ZimbraLog.event.info("deleting events for account %s (zkHost=%s)", accountId, solrHelper.getZkHost());
        if (solrHelper.needsAccountFilter()) {
            UpdateRequest req = solrHelper.newRequest(accountId);
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
            req.deleteByQuery(builder.build().toString());
            solrHelper.execute(accountId, req);
        } else {
            solrHelper.deleteIndex(accountId);
        }
    }

    @Override
    protected void deleteEventsByDataSource(String dataSourceId) throws ServiceException {
        ZimbraLog.event.info("deleting events for account %s, dsId %s (zkHost=%s)", accountId, dataSourceId, solrHelper.getZkHost());
        UpdateRequest req = solrHelper.newRequest(accountId);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (solrHelper.needsAccountFilter()) {
            builder.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
        }
        builder.add(new TermQuery(new Term(LuceneFields.L_DATASOURCE_ID, dataSourceId)), Occur.MUST);
        req.deleteByQuery(builder.build().toString());
        solrHelper.execute(accountId, req);
    }

    public static class Factory implements EventStore.Factory {

        private CloudSolrClient client;
        SolrCloudHelper solrHelper;

        private static final String CORE_NAME_OR_PREFIX = SolrEventHandlerFactory.CORE_NAME_OR_PREFIX;

        public Factory() throws ServiceException {
            Server server = Provisioning.getInstance().getLocalServer();
            SolrCollectionLocator locator;
            switch(server.getEventSolrIndexType()) {
            case account:
                locator = new AccountCollectionLocator(CORE_NAME_OR_PREFIX);
                break;
            case combined:
            default:
                locator = new JointCollectionLocator(CORE_NAME_OR_PREFIX);
                break;
            }
            String zkHost = server.getEventBackendURL().substring("solrcloud:".length());
            client = SolrUtils.getCloudSolrClient(zkHost);
            solrHelper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
            ZimbraLog.event.info("created SolrEventStore Factory with zkUrl=%s", zkHost);
        }

        @Override
        public EventStore getEventStore(String accountId) {
            return new SolrEventStore(accountId, solrHelper);
        }

        @Override
        public void shutdown() {
            try {
                solrHelper.close();
            } catch (IOException e) {
                ZimbraLog.event.error("unable to close CloudSolrClient", e);
            }

        }

    }
}
