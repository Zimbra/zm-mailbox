package com.zimbra.cs.event.logger;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.params.UpdateParams;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.SolrEventDocument;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;

public class SolrEventCallback implements BatchingEventLogger.BatchedEventCallback {

    private SolrRequestHelper helper;

    public SolrEventCallback(SolrRequestHelper helper) {
        this.helper = helper;
    }

    @Override
    public void execute(String accountId, List<Event> events) {
        UpdateRequest req = new UpdateRequest();
        req.setParam(UpdateParams.UPDATE_CHAIN, SolrConstants.SKIP_EXISTING_DOCS_UPDATE_PROCESSOR);
        for (Event event: events) {
            SolrEventDocument solrEvent = new SolrEventDocument(event);
            req.add(solrEvent.getDocument());
        }
        try {
            helper.execute(accountId, req);
        } catch (ServiceException e) {
            ZimbraLog.event.error("unable to send %d events to Solr backend", events.size(), e);
        }
    }

    @Override
    public void close() throws IOException {
        helper.close();
    }
}
