package com.zimbra.cs.index.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.IsUpdateRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;

/**
 * QueryRequest that is always sent to the leader replica, because it implements IsUpdateRequest
 * @author Greg Solovyev
 *
 */
public class LeaderQueryRequest extends QueryRequest implements IsUpdateRequest {

    private static final long serialVersionUID = 2151221957672531540L;

    public LeaderQueryRequest(ModifiableSolrParams params) {
        super(params);
    }

    public LeaderQueryRequest(SolrQuery q, METHOD post) {
        super(q, post);
    }
    
}
