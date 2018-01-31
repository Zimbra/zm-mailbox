package com.zimbra.cs.event.analytics.contact;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.mailbox.calendar.Util;

public class ContactAnalytics {
    public static enum ContactFrequencyTimeRange {
        LAST_DAY, LAST_WEEK, LAST_MONTH, FOREVER
    }

    public static enum ContactFrequencyGraphInterval {
        DAY("d"), WEEK("w");

        private String shortRepr;

        private ContactFrequencyGraphInterval(String shortRepr) {
            this.shortRepr = shortRepr;
        }
        public static ContactFrequencyGraphInterval of(String str) throws ServiceException {
            for (ContactFrequencyGraphInterval interval: ContactFrequencyGraphInterval.values()) {
                if (interval.shortRepr.equals(str)) {
                    return interval;
                }
            }
            throw ServiceException.INVALID_REQUEST(str + " is not a valid time interval", null);
        }
    }

    public static enum ContactFrequencyGraphTimeRangeUnit {
        DAY("d"), WEEK("w"), MONTH("m");

        private String shortRepr;

        private ContactFrequencyGraphTimeRangeUnit(String shortRepr) {
            this.shortRepr = shortRepr;
        }
        public static ContactFrequencyGraphTimeRangeUnit of(String str) throws ServiceException {
            for (ContactFrequencyGraphTimeRangeUnit unit: ContactFrequencyGraphTimeRangeUnit.values()) {
                if (unit.shortRepr.equals(str)) {
                    return unit;
                }
            }
            throw ServiceException.INVALID_REQUEST(str + " is not a valid time unit", null);
        }
    }

    public static enum ContactFrequencyEventType {
        SENT, RECEIVED, COMBINED
    }

    public static Long getContactFrequency(String contact, EventStore eventStore) throws ServiceException {
        return eventStore.getContactFrequencyCount(contact, ContactFrequencyEventType.COMBINED, ContactFrequencyTimeRange.FOREVER);
    }

    public static Long getContactFrequency(String contact, EventStore eventStore, ContactFrequencyEventType eventType, ContactFrequencyTimeRange timeRange) throws ServiceException {
        return eventStore.getContactFrequencyCount(contact, eventType, timeRange);
    }
    public static List<ContactFrequencyGraphDataPoint> getContactFrequencyGraph(String contact, ContactFrequencyGraphSpec graphSpec, EventStore eventStore) throws ServiceException {
            Account account = Provisioning.getInstance().getAccountById(eventStore.getAccountId());
            ICalTimeZone userTimeZone = Util.getAccountTimeZone(account);
            Integer userTimeZoneOffsetInMinutes = ((userTimeZone.getStandardOffset() / 1000) / 60);
            return getContactFrequencyGraph(contact, graphSpec, eventStore, userTimeZoneOffsetInMinutes);
    }

    public static List<ContactFrequencyGraphDataPoint> getContactFrequencyGraph(String contact, ContactFrequencyGraphSpec graphSpec, EventStore eventStore, Integer offsetInMinutes) throws ServiceException {
        return eventStore.getContactFrequencyGraph(contact, graphSpec, offsetInMinutes);
    }

    public static class ContactFrequencyGraphTimeRange {
        private ContactFrequencyGraphTimeRangeUnit unit;
        private int numUnits;
        private static Pattern pattern = Pattern.compile("\\d+");

        public ContactFrequencyGraphTimeRange(String input) throws ServiceException {
            Matcher matcher = pattern.matcher(input);
            matcher.find();
            String numStr = matcher.group();
            String rest = input.substring(matcher.end());
            try {
                numUnits = Integer.valueOf(numStr);
                if (numUnits <= 0) {
                    throw ServiceException.INVALID_REQUEST("invalid range format", null);
                }
            } catch (NumberFormatException e) {
                throw ServiceException.INVALID_REQUEST("invalid range format", null);
            }
            this.unit = ContactFrequencyGraphTimeRangeUnit.of(rest);
        }

        public ContactFrequencyGraphTimeRange(int numUnits, ContactFrequencyGraphTimeRangeUnit unit) {
            this.numUnits = numUnits;
            this.unit = unit;
        }

        public ContactFrequencyGraphTimeRangeUnit getTimeUnit() {
            return unit;
        }

        public int getNumUnits() {
            return numUnits;
        }
    }

    public static class ContactFrequencyGraphSpec {

        private ContactFrequencyGraphTimeRange range;
        private ContactFrequencyGraphInterval interval;

        public ContactFrequencyGraphSpec(ContactFrequencyGraphTimeRange range, ContactFrequencyGraphInterval interval) {
            this.range = range;
            this.interval = interval;
        }
        public ContactFrequencyGraphTimeRange getRange() {
            return range;
        }

        public ContactFrequencyGraphInterval getInterval() {
            return interval;
        }
    }
}
