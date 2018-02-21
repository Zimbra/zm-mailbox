package com.zimbra.cs.contacts;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.client.solrj.io.ModelCache;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.ComparatorOrder;
import org.apache.solr.client.solrj.io.comp.FieldComparator;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.eq.FieldEqualitor;
import org.apache.solr.client.solrj.io.graph.GatherNodesStream;
import org.apache.solr.client.solrj.io.graph.Traversal.Scatter;
import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.apache.solr.client.solrj.io.stream.IntersectStream;
import org.apache.solr.client.solrj.io.stream.MergeStream;
import org.apache.solr.client.solrj.io.stream.RankStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.metrics.CountMetric;
import org.apache.solr.client.solrj.io.stream.metrics.Metric;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.MapSolrParams;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.contacts.AffinityScope.MatchType;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityTarget;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityType;
import com.zimbra.cs.contacts.RelatedContactsResults.RelatedContact;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.SolrEventDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrUtils;
import com.zimbra.cs.mime.ParsedAddress;

/**
 * Class that builds a Solrcloud streaming query to calculate contact affinity.
 */
public class ContactAffinityQuery {

    private static final String FLD_MSG_ID = LuceneFields.L_EVENT_MESSAGE_ID;
    private static final String FLD_AFFINITY_TIMESTAMP = LuceneFields.L_EVENT_TIME;
    private static final String FLD_COUNT = String.format("count(%s)", FLD_MSG_ID);
    private static final String FLD_CONTACT_EMAIL = "node";
    private static final String SEARCH_FIELDS = String.format("%s,%s", FLD_AFFINITY_TIMESTAMP, FLD_MSG_ID);
    private static final int MODEL_CACHE_SIZE = 100;
    private static final int MAX_MESSAGES_PER_SEARCH = 1000;
    private static final int MAX_RESULTS = 200;

    private String accountId;
    private RelatedContactsParams params;
    private SolrCloudHelper solrHelper;
    private String coreName;
    private List<RelatedContact> toExclude;

    public ContactAffinityQuery(SolrCloudHelper solrHelper, RelatedContactsParams params) throws ServiceException {
        this.solrHelper = solrHelper;
        this.params = params;
        this.accountId = params.getAccountId();
        this.coreName = solrHelper.getCoreName(accountId);
        this.toExclude = new ArrayList<>();
    }

    private BooleanQuery.Builder newBooleanFilterQueryBuilder() {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (solrHelper.needsAccountFilter()) {
            SolrUtils.getAccountFilter(accountId);
            builder.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
        }
        return builder;
    }

    private void addRecipientClause(BooleanQuery.Builder builder, String recipientEmail, Occur occur) {
        String field = SolrEventDocument.getSolrQueryField(EventContextField.RECEIVER);
        String fieldValue = ClientUtils.escapeQueryChars(recipientEmail);
        Query termQuery = new TermQuery(new Term(field, fieldValue));
        builder.add(termQuery, occur);
    }

    private void addSenderClause(BooleanQuery.Builder builder, String senderEmail, Occur occur) {
        String field = SolrEventDocument.getSolrQueryField(EventContextField.SENDER);
        String fieldValue = ClientUtils.escapeQueryChars(senderEmail);
        Query termQuery = new TermQuery(new Term(field, fieldValue));
        builder.add(termQuery, occur);
    }

    private void addRecipientAndAffinityClause(BooleanQuery.Builder builder, AffinityTarget contact) {
        BooleanQuery.Builder bq = new BooleanQuery.Builder();
        addRecipientClause(bq, contact.getContactEmail(), Occur.MUST);
        addAffinityTypeClause(bq, contact.getAffinityType());
        builder.add(bq.build(), Occur.SHOULD);
    }

    private void addDateCutoffClause(BooleanQuery.Builder builder) {
        if (params.hasDateCutoff()) {
            String formatted = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(params.getDateCutoff()));
            Query rangeQuery = new TermRangeQuery(FLD_AFFINITY_TIMESTAMP, new BytesRef(formatted), new BytesRef("NOW"), true, true);
            builder.add(rangeQuery, Occur.MUST);
        }
    }

    private void addAffinityTypeClause(BooleanQuery.Builder builder, AffinityType affinityType) {
        if (affinityType != AffinityType.all) {
            String field = SolrEventDocument.getSolrQueryField(EventContextField.RECEIVER_TYPE);
            Query termQuery = new TermQuery(new Term(field, affinityType.name()));
            builder.add(termQuery, Occur.MUST);
        }
    }

    private void addEventTypeClause(BooleanQuery.Builder builder, EventType eventType) {
        String field = LuceneFields.L_EVENT_TYPE;
        Query affinityClause = new TermQuery(new Term(field, eventType.toString()));
        builder.add(affinityClause, Occur.MUST);
    }

    private TupleStream buildSearchStream(AffinityTarget target, EventType eventType, boolean ignoreAffinityType) throws IOException {
        return buildSearchStream(Arrays.asList(target), eventType, ignoreAffinityType, null);
    }

    private TupleStream buildSearchStream(List<AffinityTarget> targets, EventType eventType, boolean ignoreAffinityType) throws IOException {
        return buildSearchStream(targets, eventType, ignoreAffinityType, null);
    }

    private TupleStream buildSearchStream(List<AffinityTarget> targets, EventType eventType, boolean ignoreAffinityType, String sender) throws IOException {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        BooleanQuery.Builder filterBuilder = newBooleanFilterQueryBuilder();

        /*
         *  Requested contact emails are query clauses
         *  Affinity type is a filter clause only if one contact is provided, otherwise
         *  it's a boolean clause
         */
        if (targets.size() == 1) {
            AffinityTarget target = targets.get(0);
            addRecipientClause(queryBuilder, target.getContactEmail(), Occur.MUST);
            if (!ignoreAffinityType) {
                addAffinityTypeClause(filterBuilder, target.getAffinityType());
            }
        } else {
            //used to construct a union affinity query
            for (AffinityTarget target: targets) {
                if (ignoreAffinityType) {
                    addRecipientClause(queryBuilder, target.getContactEmail(), Occur.SHOULD);
                } else {
                    addRecipientAndAffinityClause(queryBuilder, target);
                }
            }
        }
        addEventTypeClause(filterBuilder, eventType);
        addDateCutoffClause(filterBuilder);

        if (sender != null) {
            addSenderClause(filterBuilder, sender, Occur.MUST);
        }

        String queryString = queryBuilder.build().toString();
        String filterString = filterBuilder.build().toString();

        Map<String, String> searchParams = new HashMap<>();
        searchParams.put("q", queryString);
        searchParams.put("fq", filterString);
        searchParams.put("fl", SEARCH_FIELDS);
        //limits results by both {MAX_MESSAGES_PER_SEARCH} hard cutoff and date,
        //relies on the fact that message IDs are ascending in chronological order
        searchParams.put("sort", FLD_MSG_ID + " desc");
        searchParams.put("rows", String.valueOf(MAX_MESSAGES_PER_SEARCH));

        return new CloudSolrStream(solrHelper.getZkHost(), coreName, new MapSolrParams(searchParams));
    }

    private TupleStream addNodesStream(TupleStream stream, EventType eventType) throws IOException {

        BooleanQuery.Builder filterBuilder = newBooleanFilterQueryBuilder();
        for (AffinityTarget target: params.getTargets()) {
            addRecipientClause(filterBuilder, target.getContactEmail(), Occur.MUST_NOT);
        }
        addAffinityTypeClause(filterBuilder, params.getRequestedAffinityType());
        addEventTypeClause(filterBuilder, eventType);

        String filterQuery = filterBuilder.build().toString();

        Map<String, String> params = new HashMap<>();
        params.put("fq", filterQuery);

        List<Metric> metrics = Arrays.asList(new CountMetric(FLD_MSG_ID));
        String gatherField = SolrEventDocument.getSolrStoredField(EventContextField.RECEIVER);
        Set<Scatter> scatter = Sets.newHashSet(Scatter.LEAVES);

        return new GatherNodesStream(solrHelper.getZkHost(), coreName, stream,
                FLD_MSG_ID, FLD_MSG_ID, gatherField, params, metrics, false, scatter, -1);
    }

    private TupleStream addRankStream(TupleStream stream) throws IOException {
        //Get the top 200 results, sorted by count. The vast majority of queries will return less results than this.
        //This way we can return the subset defined by params.getNumResults() and cache the rest for easy pagination.
        StreamComparator comparator = new FieldComparator(FLD_COUNT, ComparatorOrder.DESCENDING);
        return new RankStream(stream, MAX_RESULTS, comparator);
    }

    private TupleStream addIntersectStream(TupleStream stream1, TupleStream stream2) throws IOException {
        return new IntersectStream(stream1, stream2, new FieldEqualitor(FLD_MSG_ID));
    }

    private TupleStream addDedupeStream(TupleStream stream) throws IOException {
        List<String> filterValues = toExclude.stream().map(RelatedContact::getEmail).collect(Collectors.toList());
        return new ContactDedupeTupleStream(stream, FLD_CONTACT_EMAIL, FLD_COUNT, filterValues);
    }

    private TupleStream mergeStreamsOnMessageId(TupleStream... streams) throws IOException {
        return new MergeStream(new FieldComparator(FLD_MSG_ID, ComparatorOrder.DESCENDING), streams);
    }

    private TupleStream getOutgoingMessageIds(boolean matchAllTargets, boolean ignoreTargetAffinityType) throws IOException {
        List<AffinityTarget> targets = params.getTargets();
        TupleStream searchStream;
        if (matchAllTargets) {
            searchStream = buildSearchStream(targets.get(0), EventType.SENT, ignoreTargetAffinityType);
            if (targets.size() > 1) {
                //nest stream intersections
                for (int i=1; i<targets.size(); i++) {
                    searchStream = addIntersectStream(searchStream, buildSearchStream(targets.get(i), EventType.SENT, ignoreTargetAffinityType));
                }
            }
        } else {
            searchStream = buildSearchStream(targets, EventType.SENT, ignoreTargetAffinityType);
        }
        return searchStream;
    }

    private TupleStream getIncomingMessageIds(boolean senderMustBeTarget, boolean ignoreTargetAffinityField) throws IOException {
        List<AffinityTarget> targets = params.getTargets();
        TupleStream searchStream = null;
        if (!senderMustBeTarget) {
            searchStream = buildSearchStream(targets, EventType.AFFINITY, ignoreTargetAffinityField);
        } else {
            TupleStream[] toMerge = new TupleStream[targets.size()];
            int idx = 0;
            for (AffinityTarget sender: targets) {
                //treat each target as a potential sender
                String senderEmail = sender.getContactEmail();
                List<AffinityTarget> others = targets.stream().filter(
                        target -> !target.getContactEmail().equals(senderEmail)).collect(Collectors.toList());
                TupleStream stream = buildSearchStream(others, EventType.AFFINITY, ignoreTargetAffinityField, senderEmail);
                toMerge[idx] = stream;
                idx++;
            }
            searchStream = mergeStreamsOnMessageId(toMerge);
        }
        return searchStream;
    }

    private TupleStream buildAffinityStream(AffinityScope scope) throws IOException {
        EventType eventType;
        TupleStream stream;

        // 1. search for IDs of messages that satisfy the scope parameters
        boolean ignoreTargetAffinityField = scope.getIgnoreTargetAffinityField();
        switch (scope.getRelatedVia()) {
        case OUTGOING_MSGS:
            eventType = EventType.SENT;
            boolean matchAllTargets = scope.getMatchType() == MatchType.ALL;
            stream = getOutgoingMessageIds(matchAllTargets, ignoreTargetAffinityField);
            break;
        case INCOMING_MSGS:
        default:
            eventType = EventType.AFFINITY;
            stream = getIncomingMessageIds(scope.getSenderMustBeTarget(), ignoreTargetAffinityField);
            break;
        }
        // 2. walk to all other contacts referenced on these messages, counting the number of edges going to each contact
        stream = addNodesStream(stream, eventType);

        // 4. de-duplicate contacts
        stream = addDedupeStream(stream);

        // 5. sort by count
        stream = addRankStream(stream);
        return stream;
    }

    private void setStreamContext(TupleStream stream) {
        StreamContext context = new StreamContext();
        SolrClientCache clientCache = solrHelper.getClientCache();
        context.setSolrClientCache(clientCache);
        context.setModelCache(new ModelCache(MODEL_CACHE_SIZE, solrHelper.getZkHost(), clientCache));
        stream.setStreamContext(context);

    }

    private void openStream(TupleStream stream) throws ServiceException {
        try {
            stream.open();
        } catch (IOException e) {
            throw ServiceException.FAILURE("error opening contact affinity TupleStream", e);
        }
    }

    private Tuple readStream(TupleStream stream) throws ServiceException {
        try {
            return stream.read();
        } catch (IOException e) {
            throw ServiceException.FAILURE("error reading tuple from contact affinity TupleStream", e);
        }
    }

    /**
     * Compose and run a streaming expression to return related contacts for a given AffinityScope.
     */
    @VisibleForTesting
    public RelatedContactsResults execute(AffinityScope scope) throws ServiceException {
        try(TupleStream stream = buildAffinityStream(scope)) {
            setStreamContext(stream);
            openStream(stream);

            RelatedContactsResults results = new RelatedContactsResults(params, System.currentTimeMillis());
            while(true) {
                Tuple tuple = readStream(stream);
                if (tuple.EOF) {
                    return results;
                }
                String contactEmail = tuple.getString(FLD_CONTACT_EMAIL);
                double count = tuple.getDouble(FLD_COUNT);
                if (count < params.getMinOccur()) {
                    return results;
                }
                ParsedAddress parsed = new ParsedAddress(contactEmail);
                String email = parsed.emailPart;
                String name = parsed.personalPart;
                RelatedContact contact = new RelatedContact(email, count, scope.getLevel());
                if (!Strings.isNullOrEmpty(name)) {
                    contact.setName(name);
                }
                results.addResult(contact);
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to build contact affinity streaming query", e);
        }
    }

    /**
     * Starting with the most narrow ÅffinityScope and expanding outward, run related contacts queries until
     * the desired number of results is reached, or until the widest scope is searched.
     * The number of results returned by this method may exceed the value of params.getLimit(); ContactAffinityStore will
     * trim these results to the requested number. This allows us to cache more results that are returned,
     * so that a subsequent request with a higher limit may still use the cache.
     */
    public RelatedContactsResults executeWithExpandingScope() throws ServiceException {

        int minNumResults = params.getLimit();

        //First we determine which affinity scopes are possible for this query
         List<AffinityScope> scopes = AffinityScope.getAffinityScopesForQuery(params);

        //We start at the most narrow scope and expand it until we reach the number of requested results.
        //When expanding the scope, we pass in the results from the previous searches as a filter so we don't
        //double-count results.

        RelatedContactsResults results = new RelatedContactsResults(params, System.currentTimeMillis());
        AffinityScope widestScope = scopes.get(scopes.size() - 1);
        RelatedContactsResults scopeResults;
        for (AffinityScope scope: scopes) {
            if (scope == widestScope) {
                results.setMaybeHasMore(false);
            }
            scopeResults = execute(scope);
            results.addResults(scopeResults.getResults());
            if (results.size() > minNumResults) {
                //found enough results
                return results;
            } else {
                //add the results from the previous scope to the filter
                toExclude.addAll(scopeResults.getResults());
            }
        }
        //if insufficient results are found, return what we have
        return results;
    }
}
