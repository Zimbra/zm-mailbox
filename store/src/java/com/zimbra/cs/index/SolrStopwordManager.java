package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.schema.AbstractSchemaRequest;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.UpdateResponse;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.solr.SolrUtils;

public class SolrStopwordManager {

    private String collection;
    private CloudSolrClient client;
    private static final String STOPWORD_RESOURCE_URI = "/schema/analysis/stopwords/zimbra";

    public SolrStopwordManager() throws ServiceException {
        this.client = getSolrClient();
        this.collection = getMailboxCollectionName();
    }

    private CloudSolrClient getSolrClient() throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        String indexUrl = config.getIndexURL();
        if (indexUrl.startsWith("solrcloud")) {
            String zkUrls = indexUrl.substring("solrcloud:".length());
            return SolrUtils.getCloudSolrClient(zkUrls);
        } else {
            throw ServiceException.FAILURE("SolrStopwordManager can only be used with a solrcloud search backend", null);
        }
    }

    /**
     * Reload the collection; this is necessary for the stopword changes to be applied
     */
    public void reloadCollection() {
        try {
            CollectionAdminRequest.reloadCollection(collection).process(client);
        } catch (SolrServerException | IOException e) {
            ZimbraLog.index.error("unable to reload solr collection %s", collection, e);
        }
    }

    private String getMailboxCollectionName() throws ServiceException {
        try {
            Config config = Provisioning.getInstance().getConfig();
            String mboxCollectionAlias = config.getMailboxIndexName();
            Map<String, String> aliases = new CollectionAdminRequest.ListAliases().process(client).getAliases();
            String mboxCollectionName = aliases.get(mboxCollectionAlias);
            if (Strings.isNullOrEmpty(mboxCollectionName)) {
                throw ServiceException.FAILURE("unable to determine mailbox collection from alias " + mboxCollectionAlias, null);
            }
            ZimbraLog.index.info("found mailbox collection %s for alias mailbox_index", mboxCollectionName);
            return mboxCollectionName;
        } catch (SolrServerException | IOException e) {
            throw ServiceException.FAILURE("unable to get collection alias information", e);
        }
    }

    /**
     * Get the stopword list
     */
    public List<String> getStopwords() throws ServiceException {
        StopwordsRequest req = new StopwordsRequest();
        try {
            return req.process(client, collection).getStopWords();
        } catch (SolrServerException | IOException e) {
            throw ServiceException.FAILURE("unable to get list of stopwords", e);
        }
    }

    /**
     * Delete all stopwords
     */
    public void deleteAllStopwords() throws ServiceException {
        List<String> stopwords = getStopwords();
        if (stopwords.isEmpty()) {
            return;
        }
        ZimbraLog.index.info("deleting all %d stopwords", stopwords.size());
        deleteStopwords(stopwords);
    }

    /**
     * Delete one or more stopwords
     */
    public void deleteStopwords(Collection<String> wordsToDelete) throws ServiceException {
        //The DELETE http request used to delete managed stopwords doesn't seem to be thread-safe, as issuing consecutive
        //calls sometimes results in inconsistencies. As a workaround, this method introduces a slight delay
        //between each DELETE call, and does multiple passes until we are sure that all requested words have been deleted.
        Set<String> toDelete = Sets.newHashSet(wordsToDelete);
        Set<String> remainingToDelete = Sets.newHashSet(wordsToDelete); //set of words remaining to delete at each iteration
        int attempt = 1;
        do {
            ZimbraLog.index.info("deleting stopwords %s (attempt %d)", Joiner.on(",").join(remainingToDelete), attempt);
            for (String word: remainingToDelete) {
                ZimbraLog.index.debug("deleting stopword '%s'", word);
                try {
                    new DeleteStopwordRequest(word).process(client, collection);
                } catch (SolrServerException | IOException e) {
                    throw ServiceException.FAILURE(String.format("unable to delete stopword '%s'", word), e);
                }
                try {
                    Thread.sleep(100); //slight delay may help resolve race condition
                } catch (InterruptedException e) {}
            }
            //reload to pick up changes
            reloadCollection();
            Set<String> afterDeletion = Sets.newHashSet(getStopwords());
            remainingToDelete = Sets.intersection(toDelete, afterDeletion); //words that were not deleted that should have been
            attempt++;
        } while (!remainingToDelete.isEmpty());
    }

    public void addStopwords(Collection<String> stopwords) throws ServiceException {
        ZimbraLog.index.info("adding stopwords: %s", Joiner.on(",").join(stopwords));
        try {
            new AddStopwordsRequest(stopwords).process(client, collection);
        } catch (SolrServerException | IOException e) {
            throw ServiceException.FAILURE(String.format("unable to add stopwords %s", Joiner.on(",").join(stopwords)), e);
        }
    }

    private static class StopwordsResponse extends SolrResponseBase {
        @SuppressWarnings("unchecked")
        public List<String> getStopWords() {
            LinkedHashMap resp = (LinkedHashMap) getResponse().get("wordSet");
            return (List<String>) resp.get("managedList");
        }
    }

    private static class StopwordsRequest extends AbstractSchemaRequest<StopwordsResponse> {

        public StopwordsRequest() {
            super(SolrRequest.METHOD.GET, STOPWORD_RESOURCE_URI);
        }

        @Override
        protected StopwordsResponse createResponse(SolrClient client) {
            return new StopwordsResponse();
        }
    }

    private static class AddStopwordsRequest extends AbstractSchemaRequest<SchemaResponse.UpdateResponse> {

        private List<String> stopwords;

        public AddStopwordsRequest(Collection<String> stopwords) {
            super(SolrRequest.METHOD.PUT, STOPWORD_RESOURCE_URI);
            this.stopwords = new ArrayList<>(stopwords);
        }

        @Override
        protected UpdateResponse createResponse(SolrClient client) {
            return new SchemaResponse.UpdateResponse();
        }

        @Override
        public Collection<ContentStream> getContentStreams() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(stopwords);
            return Collections.singletonList(new ContentStreamBase.StringStream(json));
        }
    }

    private static class DeleteStopwordRequest extends AbstractSchemaRequest<SchemaResponse.UpdateResponse> {

        public DeleteStopwordRequest(String word) {
            super(SolrRequest.METHOD.DELETE, String.format("%s/%s", STOPWORD_RESOURCE_URI, word));
        }

        @Override
        protected UpdateResponse createResponse(SolrClient client) {
            return new SchemaResponse.UpdateResponse();
        }
    }
}
