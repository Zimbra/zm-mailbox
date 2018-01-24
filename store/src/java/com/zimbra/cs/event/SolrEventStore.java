package com.zimbra.cs.event;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.io.ModelCache;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.eq.FieldEqualitor;
import org.apache.solr.client.solrj.io.eq.MultipleFieldEqualitor;
import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.apache.solr.client.solrj.io.stream.InnerJoinStream;
import org.apache.solr.client.solrj.io.stream.JoinStream;
import org.apache.solr.client.solrj.io.stream.SelectStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.RatioMetric;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraphDataPoint;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;


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
    public Long getContactFrequencyCount(String contact, ContactAnalytics.ContactFrequencyEventType eventType, ContactAnalytics.ContactFrequencyTimeRange timeRange) throws ServiceException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(0);
        solrQuery.setQuery(getContactFrequencyQueryForTimeRange(timeRange));
        solrQuery.addFilterQuery(getQueryToSearchContact(contact, eventType));

        if (solrHelper.needsAccountFilter()) {
            solrQuery.addFilterQuery(getAccountFilter(accountId));
        }

        QueryResponse response = (QueryResponse) solrHelper.executeRequest(accountId, solrQuery);
        return response.getResults().getNumFound();
    }

    private String getQueryToSearchContact(String contact, ContactAnalytics.ContactFrequencyEventType eventType) {
        switch (eventType) {
        case SENT:
            return searchContactByEventType(contact, Event.EventType.SENT, Event.EventContextField.RECEIVER).toString();
        case RECEIVED:
            return searchContactByEventType(contact, Event.EventType.RECEIVED, Event.EventContextField.SENDER).toString();
        default:
            return getQueryToSearchContactAsSenderOrReceiver(contact).toString();
        }
    }

    private String getContactFrequencyQueryForTimeRange(ContactAnalytics.ContactFrequencyTimeRange timeRange) throws ServiceException {
        if (ContactAnalytics.ContactFrequencyTimeRange.FOREVER.equals(timeRange)) {
            return new MatchAllDocsQuery().toString();
        }
        TermRangeQuery rangeQuery = TermRangeQuery.newStringRange(LuceneFields.L_EVENT_TIME, getContactFrequencyQueryStartDate(timeRange), "NOW", true, true);
        return rangeQuery.toString();
    }

    private String getContactFrequencyQueryStartDate(ContactAnalytics.ContactFrequencyTimeRange timeRange) throws ServiceException {
        switch (timeRange) {
        case LAST_DAY:
            return "NOW-1DAY";
        case LAST_WEEK:
            return "NOW-7DAY";
        case LAST_MONTH:
            return "NOW-1MONTH";
        default:
            throw ServiceException.INVALID_REQUEST("Time range not supported " + timeRange, null);
        }
    }

    @Override
    public List<ContactFrequencyGraphDataPoint> getContactFrequencyGraph(String contact, ContactAnalytics.ContactFrequencyGraphTimeRange timeRange, Integer offsetInMinutes) throws ServiceException {
        String userSolrTimeZone = getSolrTimeZone(offsetInMinutes);
        ZonedDateTime startDate = getStartDateForContactFrequencyGraphTimeRange(timeRange, ZoneId.of(userSolrTimeZone));
        ZonedDateTime endDate = ZonedDateTime.now(ZoneId.of(userSolrTimeZone));
        String aggregationBucket = getAggregationBucketForContactFrequencyGraphTimeRange(timeRange);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(new MatchAllDocsQuery().toString());
        solrQuery.addDateRangeFacet(LuceneFields.L_EVENT_TIME, Timestamp.from(startDate.toInstant()), Timestamp.from(endDate.toInstant()), aggregationBucket);
        solrQuery.addFilterQuery(getQueryToSearchContactAsSenderOrReceiver(contact).toString());
        solrQuery.add("TZ", userSolrTimeZone);
        if (solrHelper.needsAccountFilter()) {
            solrQuery.addFilterQuery(getAccountFilter(accountId));
        }

        QueryResponse response = (QueryResponse) solrHelper.executeRequest(accountId, solrQuery);

        List<ContactFrequencyGraphDataPoint> graphDataPoints = Collections.emptyList();
        List<RangeFacet> facetRanges = response.getFacetRanges();
        if (facetRanges != null && facetRanges.size() > 0) {
            List<RangeFacet.Count> rangeFacetResult = facetRanges.get(0).getCounts();
            graphDataPoints = new ArrayList<>(rangeFacetResult.size());
            for (RangeFacet.Count rangeResult : rangeFacetResult) {
                //Need to convert the start time for range returned by Solr in UTC to Unix timestamp.
                //Solr returns time in ISO-8601 format which Instant uses by default.
                Instant instant = Instant.parse(rangeResult.getValue());
                graphDataPoints.add(new ContactFrequencyGraphDataPoint(String.valueOf(instant.toEpochMilli()), rangeResult.getCount()));
            }
        }
        return graphDataPoints;
    }

    //returns the GAP string needed by solr range facet query. It uses "GAP/ROUNDING UNIT" format.
    private String getAggregationBucketForContactFrequencyGraphTimeRange(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange) throws ServiceException {
        switch (timeRange) {
        //For current month time range we want to group the results by per day and round the start of day to midnight
        case CURRENT_MONTH:
            return "+1DAY/DAY";
        //For the last six months range we want to group the results by per week(7 days) and round the start of day to midnight.
        //Solr does not have a notion of week, since we need to express it in terms of days.
        case LAST_SIX_MONTHS:
            return "+7DAY/DAY";
        //For the current year time range we want to group the results by per month and round the start of the month to the first day of month.
        case CURRENT_YEAR:
            return "+1MONTH/MONTH";
        default:
            throw ServiceException.INVALID_REQUEST("Time range not supported " + timeRange, null);
        }
    }

    private ZonedDateTime getStartDateForContactFrequencyGraphTimeRange(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange, ZoneId userZoneId) throws ServiceException {
        switch (timeRange) {
        case CURRENT_MONTH:
            return getStartDateForCurrentMonth(userZoneId);
        case LAST_SIX_MONTHS:
            return getStartDateForLastSixMonths(userZoneId);
        case CURRENT_YEAR:
            return getStartDateForCurrentYear(userZoneId);
        default:
            throw ServiceException.INVALID_REQUEST("Time range not supported " + timeRange, null);
        }
    }

    private ZonedDateTime getStartDateForCurrentMonth(ZoneId userZoneId) {
        ZonedDateTime userTZNow = ZonedDateTime.now(userZoneId);
        return userTZNow.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
    }

    private ZonedDateTime getStartDateForLastSixMonths(ZoneId userZoneId) throws ServiceException {
        //attr id="261" name="zimbraPrefCalendarFirstDayOfWeek". sunday = 0...saturday = 6
        int firstDayOfWeek = Provisioning.getInstance().getAccountById(accountId).getPrefCalendarFirstDayOfWeek();
        //As per "zimbraPrefCalendarFirstDayOfWeek" sunday = 0...saturday = 6. But WeekFields takes days as sunday = 1....saturday = 7
        int firstDayOfWeekAdjusted = firstDayOfWeek + 1;
        ZonedDateTime userTZFirstDayOfCurrentWeekAsConfigured = ZonedDateTime.now(userZoneId).with(WeekFields.of(Locale.US).dayOfWeek(), firstDayOfWeekAdjusted).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime userTZFirstDayOfWeek6MonthsBack = userTZFirstDayOfCurrentWeekAsConfigured.minusMonths(6).with(WeekFields.of(Locale.US).dayOfWeek(), firstDayOfWeekAdjusted);
        return userTZFirstDayOfWeek6MonthsBack;
    }

    private ZonedDateTime getStartDateForCurrentYear(ZoneId userZoneId) {
        ZonedDateTime userTZNow = ZonedDateTime.now(userZoneId);
        return userTZNow.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
    }

    private String getSolrTimeZone(Integer offsetInMinutes) {
        String sign = offsetInMinutes >= 0 ? "+" : "-";
        int hours = Math.abs(offsetInMinutes) / 60;
        int minutes = Math.abs(offsetInMinutes) % 60;
        return String.format("GMT%s%02d%02d", sign, hours, minutes); //e.g. format is GMT-0500 for Eastern time zone.
    }

    private BooleanQuery getQueryToSearchContactAsSenderOrReceiver(String contact) {
        BooleanQuery sentEventWithContactInReceiverField = searchContactByEventType(contact, Event.EventType.SENT, Event.EventContextField.RECEIVER);
        BooleanQuery receivedEventWithContactInSenderField = searchContactByEventType(contact, Event.EventType.RECEIVED, Event.EventContextField.SENDER);

        BooleanQuery.Builder searchForContactAsSenderOrReceiver = new BooleanQuery.Builder();
        searchForContactAsSenderOrReceiver.add(sentEventWithContactInReceiverField, Occur.SHOULD);
        searchForContactAsSenderOrReceiver.add(receivedEventWithContactInSenderField, Occur.SHOULD);

        return searchForContactAsSenderOrReceiver.build();
    }

    private BooleanQuery searchContactByEventType(String contact, Event.EventType eventType, Event.EventContextField eventContextField) {
        TermQuery searchForContactInEventContextField = new TermQuery(new Term(SolrEventDocument.getSolrQueryField(eventContextField), contact));
        TermQuery searchForEventType = new TermQuery(new Term(LuceneFields.L_EVENT_TYPE, eventType.name()));

        BooleanQuery.Builder searchContactForAnEventTypeInAContextField = new BooleanQuery.Builder();
        searchContactForAnEventTypeInAContextField.add(searchForContactInEventContextField, Occur.MUST);
        searchContactForAnEventTypeInAContextField.add(searchForEventType, Occur.MUST);

        return searchContactForAnEventTypeInAContextField.build();
    }

    private String getAccountFilter(String accountId) {
        BooleanQuery.Builder accountFilter = new BooleanQuery.Builder();
        accountFilter.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
        return accountFilter.build().toString();
    }

    @Override
    public RatioMetric getEventTimeDelta(EventType eventType1, EventType eventType2, String contact) throws ServiceException {
        try {
            SelectStream firstEventSelectStream = getSelectStreamForEventTypeWithSearchStream(eventType1, contact);
            SelectStream secondEventSelectStream = getSelectStreamForEventTypeWithSearchStream(eventType2, contact);
            FieldEqualitor messageIdEqualitor = new FieldEqualitor(LuceneFields.L_EVENT_MESSAGE_ID);
            FieldEqualitor senderEqualitor = new FieldEqualitor(SolrEventDocument.getSolrQueryField(Event.EventContextField.SENDER));
            MultipleFieldEqualitor equalitor = new MultipleFieldEqualitor(messageIdEqualitor, senderEqualitor);
            String firstEventTimestampFieldName = getAliasForTimestampField(eventType1);
            String secondEventTimestampFieldName = getAliasForTimestampField(eventType2);
            double totalDelta = 0.0;
            int count = 0;
            try (JoinStream joinStream = new InnerJoinStream(firstEventSelectStream, secondEventSelectStream, equalitor)) {
                joinStream.setStreamContext(getStreamContext());
                joinStream.open();
                Tuple tuple = joinStream.read();
                while (!tuple.EOF) {
                    Date seenDate = tuple.getDate(firstEventTimestampFieldName);
                    Date readDate = tuple.getDate(secondEventTimestampFieldName);
                    double delta = readDate.getTime() - seenDate.getTime();
                    if (delta > 500) {
                        totalDelta += delta;
                        count++;
                    }
                    tuple = joinStream.read();
                }
            }
            return count != 0 ? new RatioMetric(totalDelta / 1000, count) : new RatioMetric(0d, 0);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to build search stream for event", e);
        }
    }

    private StreamContext getStreamContext() {
        SolrCloudHelper helper = (SolrCloudHelper) solrHelper;
        StreamContext context = new StreamContext();
        SolrClientCache clientCache = helper.getClientCache();
        context.setSolrClientCache(clientCache);
        context.setModelCache(new ModelCache(100, helper.getZkHost(), clientCache));
        return context;
    }

    private SelectStream getSelectStreamForEventTypeWithSearchStream(Event.EventType eventType, String contact) throws IOException {
        TupleStream searchStreamForEvent = getSearchStreamForEvent(eventType, contact);
        Map<String, String> fieldsWithAliasMap = getSelectStreamFieldsWithAlias(eventType);
        return new SelectStream(searchStreamForEvent, fieldsWithAliasMap);
    }

    private Map<String, String> getSelectStreamFieldsWithAlias(Event.EventType eventType) {
        Map<String, String> fieldToAliasMap = new HashMap<>(3);
        fieldToAliasMap.put(LuceneFields.L_EVENT_MESSAGE_ID, LuceneFields.L_EVENT_MESSAGE_ID);
        fieldToAliasMap.put(SolrEventDocument.getSolrQueryField(Event.EventContextField.SENDER), SolrEventDocument.getSolrQueryField(Event.EventContextField.SENDER));
        String eventTimeStampAlias = getAliasForTimestampField(eventType);
        fieldToAliasMap.put(LuceneFields.L_EVENT_TIME, eventTimeStampAlias);
        return fieldToAliasMap;
    }

    private String getAliasForTimestampField(Event.EventType eventType) {
        String alias = eventType.name() + "_timestamp";
        return alias.toLowerCase();
    }

    private TupleStream getSearchStreamForEvent(Event.EventType eventType, String contact) throws IOException {
        String query = new MatchAllDocsQuery().toString();
        String filterToGetEvents = new TermQuery(new Term(LuceneFields.L_EVENT_TYPE, eventType.name())).toString();
        String fieldsToReturn = Joiner.on(",").join(LuceneFields.L_EVENT_TIME, LuceneFields.L_EVENT_MESSAGE_ID, LuceneFields.L_EVENT_TYPE, SolrEventDocument.getSolrQueryField(Event.EventContextField.SENDER));

        String ascendingSortOnMessageId = String.format("%s %s", LuceneFields.L_EVENT_MESSAGE_ID, "asc");
        String ascendingSortOnSender = String.format("%s %s", SolrEventDocument.getSolrQueryField(Event.EventContextField.SENDER), "asc");
        String sortSequence = Joiner.on(",").join(ascendingSortOnMessageId, ascendingSortOnSender);

        NamedList<String> queryParams = new NamedList<>();
        queryParams.add("q", query);
        queryParams.add("fq", filterToGetEvents);
        queryParams.add("fl", fieldsToReturn);
        queryParams.add("sort", sortSequence);

        if (contact != null) {
            String filterToGetContact = new TermQuery(new Term(SolrEventDocument.getSolrQueryField(Event.EventContextField.SENDER), contact)).toString();
            queryParams.add("fq", filterToGetContact);
        }

        if (solrHelper.needsAccountFilter()) {
            queryParams.add("fq", getAccountFilter(accountId));
        }

        SolrCloudHelper helper = (SolrCloudHelper) solrHelper;
        return new CloudSolrStream(helper.getZkHost(), solrHelper.getCoreName(accountId), SolrParams.toSolrParams(queryParams));
    }

    @Override
    public RatioMetric getEventRatio(EventType numeratorEventType, EventType denominatorEventType, String contact) throws ServiceException {
        TermQuery searchContactInSenderField = new TermQuery(new Term(SolrEventDocument.getSolrQueryField(Event.EventContextField.SENDER), contact));

        BooleanQuery.Builder filterByEventTypes = new BooleanQuery.Builder();
        filterByEventTypes.add(new TermQuery(new Term(LuceneFields.L_EVENT_TYPE, numeratorEventType.name())), Occur.SHOULD);
        filterByEventTypes.add(new TermQuery(new Term(LuceneFields.L_EVENT_TYPE, denominatorEventType.name())), Occur.SHOULD);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(new MatchAllDocsQuery().toString());
        solrQuery.setRows(0);
        solrQuery.addFilterQuery(searchContactInSenderField.toString());
        solrQuery.addFilterQuery(filterByEventTypes.build().toString());

        if (solrHelper.needsAccountFilter()) {
            solrQuery.addFilterQuery(getAccountFilter(accountId));
        }

        solrQuery.setFacet(true);
        solrQuery.addFacetField(LuceneFields.L_EVENT_TYPE);

        QueryResponse response = (QueryResponse) solrHelper.executeRequest(accountId, solrQuery);
        if (response.getResults().getNumFound() <= 1) {
            return new RatioMetric(0d, 0);
        }

        Map<String, Long> eventFacetResults = Maps.newHashMap();
        response.getFacetFields().get(0).getValues().forEach(t -> eventFacetResults.put(t.getName(), t.getCount()));
        if (eventFacetResults.containsKey(numeratorEventType.name()) && eventFacetResults.containsKey(denominatorEventType.name())) {
            Double numerator = Double.valueOf(eventFacetResults.get(numeratorEventType.name()));
            Double denominator = Double.valueOf(eventFacetResults.get(denominatorEventType.name()));
            if (numerator.isNaN() || numerator == 0
                    || denominator.isNaN() || denominator == 0) {
                return new RatioMetric(0d, 0);
            }
            return new RatioMetric(numerator, denominator.intValue());
        }
        return new RatioMetric(0d, 0);
    }

    public abstract static class Factory implements EventStore.Factory {

        protected SolrRequestHelper solrHelper;
        protected Server server;

        public Factory() throws ServiceException {
            this.server = Provisioning.getInstance().getLocalServer();
            this.solrHelper = getRequestHelper();
        }

        protected abstract SolrRequestHelper getRequestHelper() throws ServiceException;

        protected SolrCollectionLocator getCollectionLocator() throws ServiceException {
            SolrCollectionLocator locator;
            switch (server.getEventSolrIndexType()) {
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