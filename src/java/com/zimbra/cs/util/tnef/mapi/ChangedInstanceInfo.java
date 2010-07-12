package com.zimbra.cs.util.tnef.mapi;

import java.io.IOException;
import java.util.EnumSet;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.IcalUtil;

import net.fortuna.ical4j.model.DateTime;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFUtils;

public class ChangedInstanceInfo {

    static Log sLog = ZimbraLog.tnef;

    private int exceptionNum;
    private TimeZoneDefinition tz;
    private long startMinsSince1601;
    private long endMinsSince1601;
    private long origStartMinsSince1601;
    private String subject;
    private long apptColor;
    private long subType;
    private long attachment;
    private long busyStatus;
    private String location;
    private long reminderSet;
    private long reminderDelta;
    private long meetingType;
    private EnumSet <ExceptionInfoOverrideFlag> overrideFlags;
    private long changeHighlightSize;
    private long changeHighlightValue;
    private byte[] chgHLReserved;
    private long rsrvBlockEE1Size;
    private byte[] rsrvBlockEE1;
    private long eeStartMinsSince1601;
    private long eeEndMinsSince1601;
    private long eeOrigStartMinsSince1601;
    private int unicodeSubjectLen;
    private String unicodeSubject;
    private int unicodeLocationLen;
    private String unicodeLocation;
    private long rsrvBlockEE2Size;
    private byte[] rsrvBlockEE2;

    public  ChangedInstanceInfo(int num, TimeZoneDefinition tzDef) {
        this.exceptionNum = num;
        this.tz = tzDef;
        startMinsSince1601 = 0;
        endMinsSince1601 = 0;
        origStartMinsSince1601 = 0;
        subject = null;
        apptColor =0;
        subType = 0;
        attachment =0;
        busyStatus = BusyStatus.BUSY.mapiPropValue();
        location = null;
        reminderSet = 0;
        reminderDelta = 0;
        meetingType =0;
        overrideFlags = EnumSet.noneOf(ExceptionInfoOverrideFlag.class);
        changeHighlightSize = 0;
        changeHighlightValue = 0;
        chgHLReserved = null;
        rsrvBlockEE1Size = 0;
        rsrvBlockEE1 = null;
        eeStartMinsSince1601 = -1;
        eeEndMinsSince1601 = -1;
        eeOrigStartMinsSince1601 = -1;
        unicodeSubjectLen = 0;
        unicodeSubject = null;
        unicodeLocationLen = 0;
        unicodeLocation = null;
        rsrvBlockEE2Size = 0;
        rsrvBlockEE2 = null;
    }
 
    public void readExceptionInfo(RawInputStream ris) throws IOException {
        startMinsSince1601 = ris.readU32();
        endMinsSince1601 = ris.readU32();
        origStartMinsSince1601 = ris.readU32();
        int overrides = ris.readU16();
        for (ExceptionInfoOverrideFlag flag : ExceptionInfoOverrideFlag.values()) {
            if ( (overrides & flag.mapiPropValue()) == flag.mapiPropValue()) {
                overrideFlags.add(flag);
            }
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBJECT)) {
            int subjectLenPlus1 = ris.readU16();
            int subjectLen = ris.readU16();
            if (subjectLenPlus1 != subjectLen + 1) {
                throw new IOException("Corruption near subject specification");
            }
            // TODO: This might not work for non-ISO-8859-1
            subject = ris.readString(subjectLen);
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_MEETINGTYPE)) {
            meetingType = ris.readU32();
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_REMINDERDELTA)) {
            reminderDelta = ris.readU32();
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_REMINDER)) {
            reminderSet = ris.readU32();
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_LOCATION)) {
            int locLenPlus1 = ris.readU16();
            int locLen = ris.readU16();
            if (locLenPlus1 != locLen + 1) {
                throw new IOException("Corruption near location specification");
            }
            // TODO: This might not work for non-ISO-8859-1
            location = ris.readString(locLen);
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_BUSYSTATUS)) {
            busyStatus = ris.readU32();
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_ATTACHMENT)) {
            attachment = ris.readU32();
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBTYPE)) {
            subType = ris.readU32();
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_APPTCOLOR)) {
            apptColor = ris.readU32();
        }
    }

    public void readExtendedException(RawInputStream ris,
            boolean hasChangeHighlight) throws IOException {
        if (hasChangeHighlight) {
            changeHighlightSize = ris.readU32();
            changeHighlightValue = ris.readU32();
            chgHLReserved = ris.readBytes((int)changeHighlightSize - 4);
        }
        rsrvBlockEE1Size = ris.readU32();
        rsrvBlockEE1 = ris.readBytes((int)rsrvBlockEE1Size);
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBJECT) ||
            overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_LOCATION) ) {
            eeStartMinsSince1601 = ris.readU32();
            eeEndMinsSince1601 = ris.readU32();
            eeOrigStartMinsSince1601 = ris.readU32();
            if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBJECT)) {
                unicodeSubjectLen = ris.readU16();
                unicodeSubject = ris.readStringUnicode(unicodeSubjectLen * 2);
            }
            if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_LOCATION)) {
                unicodeLocationLen = ris.readU16();
                unicodeLocation = ris.readStringUnicode(unicodeLocationLen * 2);
            }
            rsrvBlockEE2Size = ris.readU32();
            rsrvBlockEE2 = ris.readBytes((int)rsrvBlockEE2Size);
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("    ChangedInstance:")
                .append(exceptionNum).append("\n");
        this.infoOnLocalTimeSince1601Val(buf,
                "        StartDate:", startMinsSince1601);
        this.infoOnLocalTimeSince1601Val(buf,
                "        EndDate:", endMinsSince1601);
        this.infoOnLocalTimeSince1601Val(buf,
                "        OriginalStartDate:", origStartMinsSince1601);
        buf.append("        OverrideFlags:").append(overrideFlags).append("\n");

        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBJECT)) {
            buf.append("        Subject:").append(subject).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_MEETINGTYPE)) {
            buf.append("        MeetingType:").append(meetingType).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_REMINDERDELTA)) {
            buf.append("        ReminderDelta:").append(reminderDelta).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_REMINDER)) {
            buf.append("        ReminderSet:").append(reminderSet).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_LOCATION)) {
            buf.append("        Location:").append(location).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_BUSYSTATUS)) {
            buf.append("        BusyStatus:").append(this.getBusyStatus());
            buf.append(" (").append(busyStatus).append(")\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_ATTACHMENT)) {
            buf.append("        Attachment:").append(attachment).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBTYPE)) {
            buf.append("        SubType:").append(subType).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_APPTCOLOR)) {
            buf.append("        ApptColor:").append(apptColor).append("\n");
        }
        if (changeHighlightSize != 0) {
            buf.append("        changeHLSize:").append(changeHighlightSize).append("\n");
            buf.append("        changeHLValue:").append(changeHighlightValue).append("\n");
            buf.append("        chgHLReserved:")
                .append(TNEFUtils.toHexString((byte[])chgHLReserved, (int)changeHighlightSize - 4))
                .append("\n");
        }
        buf.append("        rsrvBlockEE1Size:").append(rsrvBlockEE1Size).append("\n");
        buf.append("        rsrvBlockEE1:")
            .append(TNEFUtils.toHexString((byte[])rsrvBlockEE1, (int)rsrvBlockEE1Size))
            .append("\n");
        this.infoOnLocalTimeSince1601Val(buf,
                "        ExtendedStartDate:", eeStartMinsSince1601);
        this.infoOnLocalTimeSince1601Val(buf,
                "        ExtendedEndDate:", eeEndMinsSince1601);
        this.infoOnLocalTimeSince1601Val(buf,
                "        ExtendedOriginalStartDate:", eeOrigStartMinsSince1601);
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBJECT)) {
            buf.append("        UnicodeSubject:").append(unicodeSubject).append("\n");
        }
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_LOCATION)) {
            buf.append("        UnicodeLocation:").append(unicodeLocation).append("\n");
        }
        buf.append("        rsrvBlockEE2Size:").append(rsrvBlockEE2Size).append("\n");
        buf.append("        rsrvBlockEE2:")
            .append(TNEFUtils.toHexString((byte[])rsrvBlockEE2, (int)rsrvBlockEE2Size))
            .append("\n");
        return buf.toString();
    }

    /**
     * @return the overrideFlags
     */
    public EnumSet <ExceptionInfoOverrideFlag> getOverrideFlags() {
        return overrideFlags;
    }

    /**
     * @return the apptColor
     */
    public long getApptColor() {
        return apptColor;
    }

    /**
     * @return the attachment
     */
    public long getAttachment() {
        return attachment;
    }

    /**
     * @return the busyStatus
     */
    public BusyStatus getBusyStatus() {
        if (!overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_BUSYSTATUS)) {
            return null;
        }
        for (BusyStatus curr : BusyStatus.values()) {
            if (curr.mapiPropValue() == busyStatus) {
                return curr;
            }
        }
        return null;
    }

    /**
     * @return the subject
     */
    public String getSummary() {
        // Any unicodeSubject is likely to be higher fidelity.
        if (unicodeSubject != null) {
            return unicodeSubject;
        }
        return subject;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        // Any unicodeLocation is likely to be higher fidelity.
        if (unicodeLocation != null) {
            return unicodeLocation;
        }
        return location;
    }

    /**
     * @return the meetingType
     */
    public long getMeetingType() {
        return meetingType;
    }

    /**
     * @return the reminderDelta value or null if same as for series
     */
    public Integer getReminderDelta() {
        if (!overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_REMINDERDELTA)) {
            return null;
        }
        int newDelta = (int) reminderDelta;
        return newDelta;
    }

    /**
     * @return whether a reminder was set or null if same as for series
     */
    public Boolean getReminderSet() {
        if (!overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_REMINDER)) {
            return null;
        }
        return (reminderSet != 0);
    }

    /**
     * @return if the allDayEvent flag differs from that for the recurrence, return the new value
     */
    public Boolean isAllDayEvent() {
        if (!overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBTYPE)) {
            return null;
        }
        return new Boolean(subType != 0);
    }

    /**
     * @return the startDate
     */
    public DateTime getStartDate() {
        return IcalUtil.localMinsSince1601toDate(startMinsSince1601, tz);
    }

    /**
     * @return the endDate
     */
    public DateTime getEndDate() {
        return IcalUtil.localMinsSince1601toDate(endMinsSince1601, tz);
    }

    /**
     * @return the originalStartDate
     */
    public DateTime getOriginalStartDate() {
        return IcalUtil.localMinsSince1601toDate(origStartMinsSince1601, tz);
    }

    /**
     * @return the origStartMidnightMinsSince1601
     */
    public long getOrigStartMinsSince1601() {
        return origStartMinsSince1601;
    }

    private StringBuffer infoOnLocalTimeSince1601Val(StringBuffer buf, String desc,
            long localTimeSince1601) {
        if (localTimeSince1601 == -1) {
            return buf;
        }
        buf.append(desc);
        buf.append(IcalUtil.friendlyLocalTime(localTimeSince1601, tz));
        buf.append(" (").append(IcalUtil.icalUtcTime(localTimeSince1601, tz));
        buf.append(") [");
        buf.append(localTimeSince1601);
        buf.append(" (0x");
        buf.append(Long.toHexString(localTimeSince1601));
        buf.append(")]\n");
        return buf;
    }
    
}
