package com.zimbra.cs.fb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.microsoft.schemas.exchange.services._2006.types.MapiPropertyTypeType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfPropertyValuesType;
import com.microsoft.schemas.exchange.services._2006.types.PathToExtendedFieldType;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.fb.FreeBusy.Interval;
import com.zimbra.cs.fb.FreeBusy.IntervalList;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;

public class ExchangeEWSMessage extends ExchangeMessage {

    public static final PathToExtendedFieldType PidTagScheduleInfoMonthsTentative =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagScheduleInfoFreeBusyTentative =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagScheduleInfoMonthsBusy =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagScheduleInfoFreeBusyBusy =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagScheduleInfoMonthsAway =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagScheduleInfoFreeBusyAway =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagScheduleInfoMonthsMerged =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagScheduleInfoFreeBusyMerged =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagFreeBusyPublishStart =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagFreeBusyPublishEnd =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagFreeBusyRangeTimestamp =
        new PathToExtendedFieldType();
    public static final PathToExtendedFieldType PidTagFreeBusyMessageEmailAddress =
        new PathToExtendedFieldType();

    static {
        PidTagScheduleInfoMonthsTentative.setPropertyTag("0x6851");
        PidTagScheduleInfoMonthsTentative.setPropertyType(MapiPropertyTypeType.INTEGER_ARRAY);
        PidTagScheduleInfoFreeBusyTentative.setPropertyTag("0x6852");
        PidTagScheduleInfoFreeBusyTentative.setPropertyType(MapiPropertyTypeType.BINARY_ARRAY);

        PidTagScheduleInfoMonthsBusy.setPropertyTag("0x6853");
        PidTagScheduleInfoMonthsBusy.setPropertyType(MapiPropertyTypeType.INTEGER_ARRAY);
        PidTagScheduleInfoFreeBusyBusy.setPropertyTag("0x6854");
        PidTagScheduleInfoFreeBusyBusy.setPropertyType(MapiPropertyTypeType.BINARY_ARRAY);

        PidTagScheduleInfoMonthsAway.setPropertyTag("0x6855");
        PidTagScheduleInfoMonthsAway.setPropertyType(MapiPropertyTypeType.INTEGER_ARRAY);
        PidTagScheduleInfoFreeBusyAway.setPropertyTag("0x6856");
        PidTagScheduleInfoFreeBusyAway.setPropertyType(MapiPropertyTypeType.BINARY_ARRAY);

        PidTagScheduleInfoMonthsMerged.setPropertyTag("0x684F");
        PidTagScheduleInfoMonthsMerged.setPropertyType(MapiPropertyTypeType.INTEGER_ARRAY);
        PidTagScheduleInfoFreeBusyMerged.setPropertyTag("0x6850");
        PidTagScheduleInfoFreeBusyMerged.setPropertyType(MapiPropertyTypeType.BINARY_ARRAY);

        PidTagFreeBusyPublishStart.setPropertyTag("0x6847");
        PidTagFreeBusyPublishStart.setPropertyType(MapiPropertyTypeType.INTEGER);

        PidTagFreeBusyPublishEnd.setPropertyTag("0x6848");
        PidTagFreeBusyPublishEnd.setPropertyType(MapiPropertyTypeType.INTEGER);

        PidTagFreeBusyRangeTimestamp.setPropertyTag("0x6868");
        PidTagFreeBusyRangeTimestamp.setPropertyType(MapiPropertyTypeType.SYSTEM_TIME);

        PidTagFreeBusyMessageEmailAddress.setPropertyTag("0x6849");
        PidTagFreeBusyMessageEmailAddress.setPropertyType(MapiPropertyTypeType.STRING);
    }

    public ExchangeEWSMessage(String ou, String cn, String mail) {
        super(ou, cn, mail);
    }

    Map<PathToExtendedFieldType, NonEmptyArrayOfPropertyValuesType>
        GetFreeBusyProperties(FreeBusy fb) {
        Map<PathToExtendedFieldType, NonEmptyArrayOfPropertyValuesType> ret =
            new HashMap<PathToExtendedFieldType, NonEmptyArrayOfPropertyValuesType>();
        long startMonth, endMonth;
        startMonth = millisToMonths(fb.getStartTime());
        endMonth = millisToMonths(fb.getEndTime());
        IntervalList consolidated =
            new IntervalList(fb.getStartTime(), fb.getEndTime());// TEMP

        ArrayList<String> busyMonths = new ArrayList<String>();
        ArrayList<byte[]> busyEvents = new ArrayList<byte[]>();
        ArrayList<String> tentativeMonths = new ArrayList<String>();
        ArrayList<byte[]> tentativeEvents = new ArrayList<byte[]>();
        ArrayList<String> oofMonths = new ArrayList<String>();
        ArrayList<byte[]> oofEvents = new ArrayList<byte[]>();
        ArrayList<String> allMonths = new ArrayList<String>();
        ArrayList<byte[]> allEvents = new ArrayList<byte[]>();

        encodeIntervals(fb, startMonth, endMonth, IcalXmlStrMap.FBTYPE_BUSY, busyMonths, busyEvents, consolidated);
        encodeIntervals(fb, startMonth, endMonth, IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE, tentativeMonths, tentativeEvents, consolidated);
        encodeIntervals(fb, startMonth, endMonth, IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE, oofMonths, oofEvents, consolidated);
        encodeIntervals(consolidated, startMonth, endMonth, IcalXmlStrMap.FBTYPE_BUSY, allMonths, allEvents, null);

        if (tentativeMonths.size() > 0 && tentativeEvents.size() > 0) {
            NonEmptyArrayOfPropertyValuesType nonEmptyTentativeMonths =
                new NonEmptyArrayOfPropertyValuesType();
            nonEmptyTentativeMonths.getValue().addAll(tentativeMonths);
            ret.put(PidTagScheduleInfoMonthsTentative, nonEmptyTentativeMonths);
            NonEmptyArrayOfPropertyValuesType nonEmptyTentativeEvents =
                new NonEmptyArrayOfPropertyValuesType();
            for (byte[] buf : tentativeEvents) {
                nonEmptyTentativeEvents.getValue()
                    .add(encodeBase64LittleEndian(buf));
            }
            ret.put(PidTagScheduleInfoFreeBusyTentative,
                nonEmptyTentativeEvents);
        }
        if (busyMonths.size() > 0 && busyEvents.size() > 0) {
            NonEmptyArrayOfPropertyValuesType nonEmptyBusyMonths =
                new NonEmptyArrayOfPropertyValuesType();
            nonEmptyBusyMonths.getValue().addAll(busyMonths);
            ret.put(PidTagScheduleInfoMonthsBusy, nonEmptyBusyMonths);
            NonEmptyArrayOfPropertyValuesType nonEmptyBusyEvents =
                new NonEmptyArrayOfPropertyValuesType();
            for (byte[] buf : busyEvents) {
                nonEmptyBusyEvents.getValue()
                    .add(encodeBase64LittleEndian(buf));
            }
            ret.put(PidTagScheduleInfoFreeBusyBusy, nonEmptyBusyEvents);
        }
        if (oofMonths.size() > 0 && oofEvents.size() > 0) {
            NonEmptyArrayOfPropertyValuesType nonEmptyOofMonths =
                new NonEmptyArrayOfPropertyValuesType();
            nonEmptyOofMonths.getValue().addAll(oofMonths);
            ret.put(PidTagScheduleInfoMonthsAway, nonEmptyOofMonths);
            NonEmptyArrayOfPropertyValuesType nonEmptyOofEvents =
                new NonEmptyArrayOfPropertyValuesType();
            for (byte[] buf : oofEvents) {
                nonEmptyOofEvents.getValue().add(encodeBase64LittleEndian(buf));
            }
            ret.put(PidTagScheduleInfoFreeBusyAway, nonEmptyOofEvents);
        }
        if (allMonths.size() > 0 && allEvents.size() > 0) {
            NonEmptyArrayOfPropertyValuesType nonEmptyAllMonths =
                new NonEmptyArrayOfPropertyValuesType();
            nonEmptyAllMonths.getValue().addAll(allMonths);
            ret.put(PidTagScheduleInfoMonthsMerged, nonEmptyAllMonths);
            NonEmptyArrayOfPropertyValuesType nonEmptyAllEvents =
                new NonEmptyArrayOfPropertyValuesType();
            for (byte[] buf : allEvents) {
                nonEmptyAllEvents.getValue().add(encodeBase64LittleEndian(buf));
            }
            ret.put(PidTagScheduleInfoFreeBusyMerged, nonEmptyAllEvents);
        }

        NonEmptyArrayOfPropertyValuesType fbStartTime =
            new NonEmptyArrayOfPropertyValuesType();
        fbStartTime.getValue()
            .add(String.valueOf(lMinutesSinceMsEpoch(fb.getStartTime())));
        ret.put(PidTagFreeBusyPublishStart, fbStartTime);
        NonEmptyArrayOfPropertyValuesType fbEndTime =
            new NonEmptyArrayOfPropertyValuesType();
        fbEndTime.getValue()
            .add(String.valueOf(lMinutesSinceMsEpoch(fb.getEndTime())));
        ret.put(PidTagFreeBusyPublishEnd, fbEndTime);
        NonEmptyArrayOfPropertyValuesType nonEmptyEmailAddress =
            new NonEmptyArrayOfPropertyValuesType();
        nonEmptyEmailAddress.getValue().add(mOu +
            getRcpt(LC.freebusy_exchange_cn3) + mCn);
        ret.put(PidTagFreeBusyMessageEmailAddress, nonEmptyEmailAddress);

        return ret;
    }

    private void encodeIntervals(Iterable<Interval> fb, long startMonth,
        long endMonth, String type, ArrayList<String> months,
        ArrayList<byte[]> events, IntervalList consolidated) {
        HashMap<Long, LinkedList<Byte>> fbMap =
            new HashMap<Long, LinkedList<Byte>>();
        for (long i = startMonth; i <= endMonth; i++)
            fbMap.put(i, new LinkedList<Byte>());
        for (FreeBusy.Interval interval : fb) {
            String status = interval.getStatus();
            if (status.equals(type)) {
                LinkedList<Byte> buf =
                    fbMap.get(millisToMonths(interval.getStart()));
                encodeFb(interval.getStart(), interval.getEnd(), buf);
                if (consolidated != null)
                    consolidated.addInterval(new Interval(interval.getStart(),
                        interval.getEnd(),
                        IcalXmlStrMap.FBTYPE_BUSY));
            }
        }
        for (long m = startMonth; m <= endMonth; m++) {
            LinkedList<Byte> encodedList = fbMap.get(m);
            byte[] raw = {};
            if (encodedList.size() > 0) {
                try {
                    raw = new byte[encodedList.size()];
                    for (int i = 0; i < encodedList.size(); i++)
                        raw[i] = encodedList.get(i).byteValue();
                } catch (Exception e) {
                    ZimbraLog.fb.warn("error converting millis to minutes for month " +
                        m,
                        e);
                    continue;
                }
            }
            if (0 != raw.length) {
                months.add(Long.toString(m));
                events.add(raw);
            }
        }
    }

    protected Long lMinutesSinceMsEpoch(long millis) {
        // filetime or ms epoch is calculated as minutes since Jan 1 1601
        // standard epoch is Jan 1 1970. the offset in seconds is
        // 11644473600
        Long mins = (millis / 1000 + 11644473600L) / 60;

        return mins;
    }

    public static String encodeBase64LittleEndian(byte[] vby) {
        final String charSet =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        final byte[] encodeData = new byte[64];
        for (int i = 0; i < 64; i++) {
            byte c = (byte)charSet.charAt(i);
            encodeData[i] = c;
        }
        int length = vby.length;
        int start = 0;
        byte[] dst = new byte[(length + 2) / 3 * 4 + length / 72];
        int x = 0;
        int dstIndex = 0;
        int state = 0; // which char in pattern
        int old = 0; // previous byte
        int len = 0; // length decoded so far
        int max = length + start;
        for (int srcIndex = start; srcIndex < max; srcIndex++) {
            x = vby[srcIndex];
            switch (++state) {
            case 1:
                dst[dstIndex++] = encodeData[(x >> 2) & 0x3f];
                break;
            case 2:
                dst[dstIndex++] =
                    encodeData[((old << 4) & 0x30) | ((x >> 4) & 0xf)];
                break;
            case 3:
                dst[dstIndex++] =
                    encodeData[((old << 2) & 0x3C) | ((x >> 6) & 0x3)];
                dst[dstIndex++] = encodeData[x & 0x3F];
                state = 0;
                break;
            }
            old = x;
            if (++len >= 72) {
                dst[dstIndex++] = (byte)'\n';
                len = 0;
            }
        }
        /*
         * now clean up the end bytes
         */

        switch (state) {
        case 1:
            dst[dstIndex++] = encodeData[(old << 4) & 0x30];
            dst[dstIndex++] = (byte)'=';
            dst[dstIndex++] = (byte)'=';
            break;
        case 2:
            dst[dstIndex++] = encodeData[(old << 2) & 0x3c];
            dst[dstIndex++] = (byte)'=';
            break;
        }
        return new String(dst);

    }
}
