package com.zimbra.cs.event.analytics.contact;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.EventStore;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ContactAnalytics {
    public enum TimeRange {
        LAST_DAY("NOW-1DAY", "NOW", "+1HOUR", "HOUR"),
        LAST_WEEK("NOW-7DAY", "NOW", "+1DAY", "DAY"),
        LAST_MONTH("NOW-1MONTH", "NOW", "+1DAY", "DAY"),
        CURRENT_MONTH("+1DAY", "DAY"),
        LAST_SIX_MONTHS("+7DAY", "DAY"),
        CURRENT_YEAR("+1MONTH", "MONTH"),
        FOREVER("1970-01-01T00:00:00.001Z", "NOW", "+1YEAR", "YEAR");

        private String solrStartDate = "";
        private String solrEndDate = "";
        private String solrAggregationUnit;
        private String solrAggregationRoundingUnit;

        TimeRange(String solrAggregationUnit, String solrAggregationRoundingUnit) {
            this.solrAggregationUnit = solrAggregationUnit;
            this.solrAggregationRoundingUnit = solrAggregationRoundingUnit;
        }

        TimeRange(String solrStartDate, String solrEndDate, String solrAggregationUnit, String solrAggregationRoundingUnit) {
            this.solrStartDate = solrStartDate;
            this.solrEndDate = solrEndDate;
            this.solrAggregationUnit = solrAggregationUnit;
            this.solrAggregationRoundingUnit = solrAggregationRoundingUnit;
        }

        public String getSolrStartDate() {
            return solrStartDate;
        }

        public String getSolrEndDate() {
            return solrEndDate;
        }

        public String getSolrAggregationUnit() {
            return solrAggregationUnit;
        }

        public String getSolrAggregationRoundingUnit() {
            return solrAggregationRoundingUnit;
        }

        public String getSolrAggregationBucket() {
            return solrAggregationUnit + "/" + solrAggregationRoundingUnit;
        }
    }

    public enum ContactFrequencyEventType {
        SENT, RECEIVED, COMBINED
    }

    public static Long getContactFrequency(String contact, EventStore eventStore) throws ServiceException {
        return eventStore.getContactFrequencyCount(contact, ContactFrequencyEventType.COMBINED, TimeRange.FOREVER);
    }

    public static Long getContactFrequency(String contact, EventStore eventStore, ContactFrequencyEventType eventType, TimeRange timeRange) throws ServiceException {
        return eventStore.getContactFrequencyCount(contact, eventType, timeRange);
    }

    public static List<ContactFrequencyGraphDataPoint> getContactFrequencyGraph(String contact, ContactAnalytics.TimeRange timeRange, EventStore eventStore) throws ServiceException {
        return eventStore.getContactFrequencyGraph(contact, timeRange);
    }
}
