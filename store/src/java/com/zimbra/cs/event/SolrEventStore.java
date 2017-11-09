package com.zimbra.cs.event;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.UpdateRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import org.apache.solr.client.solrj.response.QueryResponse;


/**
 * Base class for SolrCloud / Standalone Solr event backends
 */
public abstract class SolrEventStore extends EventStore {

    protected SolrRequestHelper solrHelper;

    public SolrEventStore(String accountId, SolrRequestHelper solrHelper) {
        super(accountId);
        this.solrHelper = solrHelper;
    }

    @Override
    protected void deleteEventsByAccount() throws ServiceException {
        ZimbraLog.event.info("deleting events for account %s", accountId);
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
        ZimbraLog.event.info("deleting events for account %s, dsId %s", accountId, dataSourceId);
        UpdateRequest req = solrHelper.newRequest(accountId);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (solrHelper.needsAccountFilter()) {
            builder.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
        }
        builder.add(new TermQuery(new Term(LuceneFields.L_DATASOURCE_ID, dataSourceId)), Occur.MUST);
        req.deleteByQuery(builder.build().toString());
        solrHelper.execute(accountId, req);
    }

    @Override
    public Long getContactFrequencyCount(String contact) throws ServiceException {
        BooleanQuery.Builder searchForContactAsSenderOrReceiver = new BooleanQuery.Builder();
        searchForContactAsSenderOrReceiver.add(getQueryToSearchContactAsSenderOrReceiver(contact), Occur.MUST);
        if (solrHelper.needsAccountFilter()) {
            searchForContactAsSenderOrReceiver.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(searchForContactAsSenderOrReceiver.build().toString());

        QueryResponse response = (QueryResponse) solrHelper.executeRequest(accountId, solrQuery);
        return response.getResults().getNumFound();
    }

    private BooleanQuery getQueryToSearchContactAsSenderOrReceiver(String contact) {
        BooleanQuery sentEventWithContactInReceiverField = searchContactForAnEventTypeInAContextField(contact, Event.EventType.SENT, Event.EventContextField.RECEIVER);
        BooleanQuery receivedEventWithContactInSenderField = searchContactForAnEventTypeInAContextField(contact, Event.EventType.RECEIVED, Event.EventContextField.SENDER);

        BooleanQuery.Builder searchForContactAsSenderOrReceiver = new BooleanQuery.Builder();
        searchForContactAsSenderOrReceiver.add(sentEventWithContactInReceiverField, Occur.SHOULD);
        searchForContactAsSenderOrReceiver.add(receivedEventWithContactInSenderField, Occur.SHOULD);

        return searchForContactAsSenderOrReceiver.build();
    }

    private BooleanQuery searchContactForAnEventTypeInAContextField(String contact, Event.EventType eventType, Event.EventContextField eventContextField) {
        TermQuery searchForContactInEventContextField = new TermQuery(new Term(SolrEventDocument.getSolrField(eventContextField), contact));
        TermQuery searchForEventType = new TermQuery(new Term(LuceneFields.L_EVENT_TYPE, eventType.name()));

        BooleanQuery.Builder searchContactForAnEventTypeInAContextField = new BooleanQuery.Builder();
        searchContactForAnEventTypeInAContextField.add(searchForContactInEventContextField, Occur.MUST);
        searchContactForAnEventTypeInAContextField.add(searchForEventType, Occur.MUST);

        return searchContactForAnEventTypeInAContextField.build();
    }

    public static class Factory implements EventStore.Factory {

        protected SolrRequestHelper solrHelper;
        protected Server server;

        public Factory() throws ServiceException {
            this.server = Provisioning.getInstance().getLocalServer();
            this.solrHelper = getRequestHelper();
        }

        protected abstract SolrRequestHelper getRequestHelper() throws ServiceException;

        protected SolrCollectionLocator getCollectionLocator() throws ServiceException {
            SolrCollectionLocator locator;
            switch(server.getEventSolrIndexType()) {
            case account:
                locator = new AccountCollectionLocator(SolrConstants.EVENT_CORE_NAME_OR_PREFIX);
                break;
            case combined:
            default:
                locator = new JointCollectionLocator(SolrConstants.EVENT_CORE_NAME_OR_PREFIX);
                break;
            }
            return locator;
        }

        @Override
        public void shutdown() {
            try {
                solrHelper.close();
            } catch (IOException e) {
                ZimbraLog.event.error("unable to close SolrRequestHelper", e);
            }
        }
    }
}
