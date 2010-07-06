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
    private long startMidnightMinsSince1601;
    private long endMidnightMinsSince1601;
    private long origStartMidnightMinsSince1601;
    private DateTime startDate;
    private DateTime endDate;
    private DateTime originalStartDate;
    private String subject;
    private long apptColor;
    private long subType;
    private long attachment;
    private long busyStatus;
    private String location;
    private long reminderSet;
    private long reminderDelta;
    private long meetingType;
    EnumSet <ExceptionInfoOverrideFlag> overrideFlags;
    long changeHighlightSize;
    long changeHighlightValue;
    byte[] chgHLReserved;
    long rsrvBlockEE1Size;
    byte[] rsrvBlockEE1;
    private DateTime eeStartDate;
    private DateTime eeEndDate;
    private DateTime eeOriginalStartDate;
    int unicodeSubjectLen;
    String unicodeSubject;
    int unicodeLocationLen;
    String unicodeLocation;
    long rsrvBlockEE2Size;
    byte[] rsrvBlockEE2;

    public  ChangedInstanceInfo(int num) {
        this.exceptionNum = num;
        startMidnightMinsSince1601 = 0;
        endMidnightMinsSince1601 = 0;
        origStartMidnightMinsSince1601 = 0;
        startDate = null;
        endDate = null;
        originalStartDate = null;
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
        eeStartDate = null;
        eeEndDate = null;
        eeOriginalStartDate = null;
        unicodeSubjectLen = 0;
        unicodeSubject = null;
        unicodeLocationLen = 0;
        unicodeLocation = null;
        rsrvBlockEE2Size = 0;
        rsrvBlockEE2 = null;
    }
 
    public void readExceptionInfo(RawInputStream ris,
            TimeZoneDefinition tz) throws IOException {
        startMidnightMinsSince1601 = ris.readU32();
        startDate = IcalUtil.localMinsSince1601toDate(startMidnightMinsSince1601, tz);
        endMidnightMinsSince1601 = ris.readU32();
        endDate = IcalUtil.localMinsSince1601toDate(endMidnightMinsSince1601, tz);
        origStartMidnightMinsSince1601 = ris.readU32();
        originalStartDate = IcalUtil.localMinsSince1601toDate(
                                origStartMidnightMinsSince1601, tz);
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
            // TODO: This probably won't work for non-ISO-8859-1
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
            // TODO: This probably won't work for non-ISO-8859-1
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
            TimeZoneDefinition tz, boolean hasChangeHighlight) throws IOException {
        if (hasChangeHighlight) {
            changeHighlightSize = ris.readU32();
            changeHighlightValue = ris.readU32();
            chgHLReserved = ris.readBytes((int)changeHighlightSize - 4);
        }
        rsrvBlockEE1Size = ris.readU32();
        rsrvBlockEE1 = ris.readBytes((int)rsrvBlockEE1Size);
        if (overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_SUBJECT) ||
            overrideFlags.contains(ExceptionInfoOverrideFlag.ARO_LOCATION) ) {
            long startMidnightMinsSince1601 = ris.readU32();
            eeStartDate = IcalUtil.localMinsSince1601toDate(startMidnightMinsSince1601, tz);
            long endMidnightMinsSince1601 = ris.readU32();
            eeEndDate = IcalUtil.localMinsSince1601toDate(endMidnightMinsSince1601, tz);
            long origStartMidnightMinsSince1601 = ris.readU32();
            eeOriginalStartDate = IcalUtil.localMinsSince1601toDate(
                                    origStartMidnightMinsSince1601, tz);
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
        if (startDate != null) {
            buf.append("        StartDate:").append(startDate).append(" (");
            buf.append(startMidnightMinsSince1601).append(")\n");
        }
        if (endDate != null) {
            buf.append("        EndDate:").append(endDate).append(" (");
            buf.append(endMidnightMinsSince1601).append(")\n");
        }
        if (originalStartDate != null) {
            buf.append("        OriginalStartDate:").append(originalStartDate).append(" (");
            buf.append(origStartMidnightMinsSince1601).append(")\n");
        }
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
        if (eeStartDate != null) {
            buf.append("        ExtendedStartDate:").append(eeStartDate).append("\n");
        }
        if (eeEndDate != null) {
            buf.append("        ExtendedEndDate:").append(eeEndDate).append("\n");
        }
        if (eeOriginalStartDate != null) {
            buf.append("        ExtendedOriginalStartDate:").append(eeOriginalStartDate).append("\n");
        }
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
        return startDate;
    }

    /**
     * @return the endDate
     */
    public DateTime getEndDate() {
        return endDate;
    }

    /**
     * @return the originalStartDate
     */
    public DateTime getOriginalStartDate() {
        return originalStartDate;
    }

    /**
     * @return the origStartMidnightMinsSince1601
     */
    public long getOrigStartMidnightMinsSince1601() {
        return origStartMidnightMinsSince1601;
    }
}
