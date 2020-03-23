package com.zimbra.cs.event;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.contacts.ContactAffinityQuery;
import com.zimbra.cs.contacts.RelatedContactsParams;
import com.zimbra.cs.contacts.RelatedContactsResults;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

public class SolrCloudEventStore extends SolrEventStore {

    public SolrCloudEventStore(String accountId, SolrCloudHelper solrHelper) {
        super(accountId, solrHelper);
    }

    @Override
    public RelatedContactsResults getContactAffinity(RelatedContactsParams params) throws ServiceException {
        ContactAffinityQuery query = new ContactAffinityQuery((SolrCloudHelper) solrHelper, params);
        return query.executeWithExpandingScope();
    }

    public static class Factory extends SolrEventStore.Factory {

        private CloudSolrClient client;

        public Factory() throws ServiceException {
            super();
        }

        @Override
        public EventStore getEventStore(String accountId) {
            return new SolrCloudEventStore(accountId, (SolrCloudHelper) solrHelper);
        }

        @Override
        protected SolrRequestHelper getRequestHelper() throws ServiceException {
            String zkHost = server.getEventBackendURL().substring("solrcloud:".length());
            client = SolrUtils.getCloudSolrClient(zkHost);
            return new SolrCloudHelper(getCollectionLocator(), client, MailboxIndex.IndexType.EVENTS);
        }
    }
}
