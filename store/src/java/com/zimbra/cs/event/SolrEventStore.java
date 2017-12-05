package com.zimbra.cs.event;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.NotImplementedException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraphDataPoint;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
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
        if(ContactAnalytics.ContactFrequencyTimeRange.FOREVER.equals(timeRange)) {
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
    public List<ContactFrequencyGraphDataPoint> getContactFrequencyGraph(String contact, ContactAnalytics.ContactFrequencyGraphTimeRange timeRange) throws ServiceException {
        LocalDateTime startDate = getStartDateForContactFrequencyGraphTimeRange(timeRange);
        LocalDateTime endDate = LocalDateTime.now();
        String aggregationBucket = getAggregationBucketForContactFrequencyGraphTimeRange(timeRange);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(new MatchAllDocsQuery().toString());
        solrQuery.addDateRangeFacet(LuceneFields.L_EVENT_TIME, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate), aggregationBucket);
        solrQuery.addFilterQuery(getQueryToSearchContactAsSenderOrReceiver(contact).toString());

        if (solrHelper.needsAccountFilter()) {
            solrQuery.addFilterQuery(getAccountFilter(accountId));
        }

        QueryResponse response = (QueryResponse) solrHelper.executeRequest(accountId, solrQuery);

        List<ContactFrequencyGraphDataPoint> graphDataPoints = Collections.emptyList();
        List<RangeFacet> facetRanges = response.getFacetRanges();
        if(facetRanges != null && facetRanges.size() > 0) {
            List<RangeFacet.Count> rangeFacetResult = facetRanges.get(0).getCounts();
            graphDataPoints = new ArrayList<>(rangeFacetResult.size());
            for (RangeFacet.Count rangeResult : rangeFacetResult) {
                graphDataPoints.add(new ContactFrequencyGraphDataPoint(rangeResult.getValue(), rangeResult.getCount()));
            }
        }
        return graphDataPoints;
    }

    private String getAggregationBucketForContactFrequencyGraphTimeRange(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange) throws ServiceException {
        switch (timeRange) {
        case CURRENT_MONTH:
            return "+1DAY/DAY";
        case LAST_SIX_MONTHS:
            return "+7DAY/DAY";
        case CURRENT_YEAR:
            return "+1MONTH/MONTH";
        default:
            throw ServiceException.INVALID_REQUEST("Time range not supported " + timeRange, null);
        }
    }

    private LocalDateTime getStartDateForContactFrequencyGraphTimeRange(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange) throws ServiceException {
        switch (timeRange) {
        case CURRENT_MONTH:
            return getStartDateForCurrentMonth();
        case LAST_SIX_MONTHS:
            return getStartDateForLastSixMonths();
        case CURRENT_YEAR:
            return getStartDateForCurrentyear();
        default:
            throw ServiceException.INVALID_REQUEST("Time range not supported " + timeRange, null);
        }
    }

    private LocalDateTime getStartDateForCurrentMonth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
        return firstDayOfMonth;
    }

    private LocalDateTime getStartDateForLastSixMonths() throws ServiceException {
        //attr id="261" name="zimbraPrefCalendarFirstDayOfWeek". sunday = 0...saturday = 6
        int firstDayOfWeek = Provisioning.getInstance().getAccountById(accountId).getPrefCalendarFirstDayOfWeek();
        //As per "zimbraPrefCalendarFirstDayOfWeek" sunday = 0...saturday = 6. But WeekFields takes days as sunday = 1....saturday = 7
        int firstDayOfWeekAdjusted = firstDayOfWeek + 1;
        LocalDateTime firstDayOfCurrentWeekAsConfigured = LocalDateTime.now().with(WeekFields.of(Locale.US).dayOfWeek(), firstDayOfWeekAdjusted).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime firstDayOfWeek6MonthsBack = firstDayOfCurrentWeekAsConfigured.minusMonths(6).with(WeekFields.of(Locale.US).dayOfWeek(), firstDayOfWeekAdjusted);
        return firstDayOfWeek6MonthsBack;
    }

    private LocalDateTime getStartDateForCurrentyear() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfCurrentYear = now.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
        return firstDayOfCurrentYear;
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