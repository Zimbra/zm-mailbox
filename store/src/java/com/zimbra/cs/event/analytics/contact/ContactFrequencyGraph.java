package com.zimbra.cs.event.analytics.contact;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.logger.EventStore;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ContactFrequencyGraph {
    public enum TimeRange {
        CURRENT_MONTH("+1DAY", "DAY"),
        LAST_SIX_MONTHS("+7DAY", "DAY"),
        CURRENT_YEAR("+1MONTH", "MONTH");

        private String solrAggregationUnit;
        private String solrAggregationRoundingUnit;

        TimeRange(String solrAggregationUnit, String solrAggregationRoundingUnit) {
            this.solrAggregationUnit = solrAggregationUnit;
            this.solrAggregationRoundingUnit = solrAggregationRoundingUnit;
        }

        public String getSolrAggregationUnit() { return solrAggregationUnit; }
        public String getSolrAggregationRoundingUnit() { return solrAggregationRoundingUnit; }
        public String getSolrAggregationBucket() { return solrAggregationUnit + "/" + solrAggregationRoundingUnit; }
    }

    public static List<ContactFrequencyGraphDataPoint> getContactFrequencyGraph(String contact, TimeRange timeRange, EventStore eventStore) throws ServiceException {
        switch (timeRange) {
            case CURRENT_MONTH:
                return getContactFrequencyGraphForCurrentMonth(contact, eventStore);
            case LAST_SIX_MONTHS:
                //attr id="261" name="zimbraPrefCalendarFirstDayOfWeek". sunday = 0...saturday = 6
                int firstDayOfWeek = Provisioning.getInstance().getAccountById(eventStore.accountId).getPrefCalendarFirstDayOfWeek();
                return getContactFrequencyGraphForLastSixMonths(contact, eventStore, firstDayOfWeek);
            case CURRENT_YEAR:
                return getContactFrequencyGraphForCurrentYear(contact, eventStore);
            default:
                return Collections.emptyList();
        }
    }

    private static List<ContactFrequencyGraphDataPoint> getContactFrequencyGraphForCurrentMonth(String contact, EventStore eventStore) throws ServiceException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
        String aggregationBucket = TimeRange.CURRENT_MONTH.getSolrAggregationBucket();
        return eventStore.getContactFrequencyGraph(contact, java.sql.Timestamp.valueOf(firstDayOfMonth), java.sql.Timestamp.valueOf(now), aggregationBucket);
    }

    private static List<ContactFrequencyGraphDataPoint> getContactFrequencyGraphForLastSixMonths(String contact, EventStore eventStore, int firstDayOfWeek) throws ServiceException {
        //As per "zimbraPrefCalendarFirstDayOfWeek" sunday = 0...saturday = 6. But WeekFields takes days as sunday = 1....saturday = 7
        int firstDayOfWeekAdjusted = firstDayOfWeek + 1;
        LocalDateTime firstDayOfCurrentWeekAsConfigured = LocalDateTime.now().with(WeekFields.of(Locale.US).dayOfWeek(), firstDayOfWeekAdjusted).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime firstDayOfWeek6MonthsBack = firstDayOfCurrentWeekAsConfigured.minusMonths(6).with(WeekFields.of(Locale.US).dayOfWeek(), firstDayOfWeekAdjusted);
        String aggregationBucket = TimeRange.LAST_SIX_MONTHS.getSolrAggregationBucket();
        return eventStore.getContactFrequencyGraph(contact, java.sql.Timestamp.valueOf(firstDayOfWeek6MonthsBack), java.sql.Timestamp.valueOf(firstDayOfCurrentWeekAsConfigured), aggregationBucket);
    }

    private static List<ContactFrequencyGraphDataPoint> getContactFrequencyGraphForCurrentYear(String contact, EventStore eventStore) throws ServiceException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfCurrentYear = now.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
        String aggregationBucket = TimeRange.CURRENT_YEAR.getSolrAggregationBucket();
        return eventStore.getContactFrequencyGraph(contact, java.sql.Timestamp.valueOf(firstDayOfCurrentYear), java.sql.Timestamp.valueOf(now), aggregationBucket);
    }


}
