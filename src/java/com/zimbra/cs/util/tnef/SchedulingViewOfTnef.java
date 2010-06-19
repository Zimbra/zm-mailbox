/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util.tnef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zimbra.cs.util.tnef.mapi.GlobalObjectId;
import com.zimbra.cs.util.tnef.mapi.RecurrenceDefinition;
import com.zimbra.cs.util.tnef.mapi.TimeZoneDefinition;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.Clazz;
import net.freeutils.tnef.Attachment;
import net.freeutils.tnef.Attr;
import net.freeutils.tnef.GUID;
import net.freeutils.tnef.MAPIProp;
import net.freeutils.tnef.MAPIPropName;
import net.freeutils.tnef.MAPIProps;
import net.freeutils.tnef.MAPIValue;
import net.freeutils.tnef.Message;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFInputStream;
import net.freeutils.tnef.TNEFUtils;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.mapi.AppointmentStateFlags;
import com.zimbra.cs.util.tnef.mapi.BusyStatus;
import java.util.EnumSet;
/**
 * The <code>SchedulingViewOfTnef</code> class encapsulates the Scheduling related
 * aspects of a TNEF file.
 *
 * A cut down version of net.freeutils.tnef.Message which ignores attributes
 * and attachments which are not regarded as important for Zimbra scheduling.
 *
 * @author Gren Elliot
 */
public class SchedulingViewOfTnef extends Message {

    static Log sLog = ZimbraLog.tnef;

    private int xmlIndentLevel;
    private String messageClass;
    private GlobalObjectId globalObjectId;
    private GlobalObjectId cleanGlobalObjectId;
    private EnumSet <AppointmentStateFlags> appointmentStateFlagsMask;

    public SchedulingViewOfTnef() {
        List <Attr> attribs = new ArrayList <Attr>();
        List <Attachment> attaches = new ArrayList <Attachment>();
        setAttributes(attribs);
        setAttachments(attaches);
        messageClass = null;
        globalObjectId = null;
        cleanGlobalObjectId = null;
        appointmentStateFlagsMask = null;
    }

    /**
     * Constructs a SchedulingView of the attributes in the supplied
     * TNEFInputStream.
     *
     * @param in the TNEFInputStream containing message data
     * @throws IOException if an I/O error occurs
     */
    public SchedulingViewOfTnef(TNEFInputStream in) throws IOException {
        this();
        read(in);
    }

    /**
     * Gathers scheduling related information from the TNEFInputStream.
     *
     * @param in the TNEFInputStream containing message data
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void read(TNEFInputStream in) throws IOException {
        Attr attr;
        while ((attr = in.readAttr()) != null) {
            switch (attr.getLevel()) {
                case Attr.LVL_ATTACHMENT:
                    break;
                case Attr.LVL_MESSAGE:
                    if (isNeededAttribute(attr)) {
                        super.addAttribute(attr);
                    }
                    break;
                default:
                    throw new IOException("Invalid attribute level: " + attr.getLevel());
            } // switch level
        } // while
    }

    /**
     * Tests if an interest has been registered in this attribute
     * TODO: Actually provide a filtering mechanism...  This is intended
     * mostly to save memory caching attributes we don't care about.
     *
     * @param attr the Attribute to test containing message data
     * @return
     * @throws IOException if an I/O error occurs
     */
    protected boolean isNeededAttribute(Attr attr) throws IOException {
        return true;
    }

    /**
     * @return the value of the attMessageClass TNEF attribute
     * @throws IOException
     */
    public String getMessageClass() throws IOException {
        if (messageClass != null) {
            return messageClass;
        }
        Attr attMsgClass = getAttr(Attr.attMessageClass);
        if (attMsgClass == null) {
            return null;
        }
        messageClass = attMsgClass.getValue().toString();
        return messageClass;
    }

    /**
     * @return A valid ICAL UID
     * @throws IOException
     */
    public String getIcalUID() throws IOException {
        // Canonical name: PidLidCleanGlobalObjectId
        // Description: Contains the value of the PidLidGlobalObjectId property for an object that represents
        // an exception to a recurring series, where the Year, Month, and Day fields are all zero.
        // Property set: PSETID_Meeting {6ED8DA90-450B-101B-98DA-00AA003F1305}
        // Property long ID (LID): 0x00000023
        // Canonical name: PidLidGlobalObjectId
        // Description: Contains the value of the PidLidGlobalObjectId property for an object that represents
        // an exception to a recurring series.
        // Property set: PSETID_Meeting {6ED8DA90-450B-101B-98DA-00AA003F1305}
        // Property long ID (LID): 0x00000003
        // Data type: PtypBinary, 0x0102
        RawInputStream ris;
        if (globalObjectId == null) {
            MAPIPropName pName = new MAPIPropName(MSGUID.PSETID_Meeting.getJtnefGuid(), (long)0x23);
            ris = getRawInputStreamValue(pName, 0);
            if (ris != null) {
                globalObjectId = new GlobalObjectId(ris);
            }
        }
        if (globalObjectId != null) {
            return globalObjectId.getIcalUid();
        }
        if (cleanGlobalObjectId == null) {
            MAPIPropName pName = new MAPIPropName(MSGUID.PSETID_Meeting.getJtnefGuid(), (long)0x3);
            ris = getRawInputStreamValue(pName, 0);
            if (ris != null) {
                cleanGlobalObjectId = new GlobalObjectId(ris);
            }
        }
        if (cleanGlobalObjectId != null) {
            return cleanGlobalObjectId.getIcalUid();
        }

        return null;
    }

    /**
     * @return A valid ICAL Sequence Number - 0 if the underlying property is absent.
     * @throws IOException
     */
    public Integer getSequenceNumber() throws IOException {
        // PidLidAppointmentSequence - Specifies the sequence number of a Meeting object.
        MAPIPropName PidLidAppointmentSequence = PSETID_AppointmentPropName(0x8201);
        Integer sequenceNum = getIntegerValue(PidLidAppointmentSequence, 0);
        if (sequenceNum == null) {
            sequenceNum = new Integer(0);
        }
        return sequenceNum;
    }

    /**
     * @return Value of PidTagImportance property
     * @throws IOException
     */
    public Integer getMapiImportance() throws IOException {
        return getIntegerValue(null, 0x17);
    }

    /**
     * The PidTagOwnerAppointmentId has the alternate name PR_OWNER_APPT_ID
     * @return Value of PidTagOwnerAppointmentId
     * @throws IOException
     */
    public Integer getOwnerAppointmentId() throws IOException {
        return getIntegerValue(null, 0x62);
    }

    /**
     * Canonical name: PidTagSensitivity - Property ID: 0x0036
     *    MS-OXICAL recommends ICAL Class <--> PidTagSensitivity mapping :
     *        CLASS           PidTagSensitivity
     *        PRIVATE         0x00000002
     *        CONFIDENTIAL    0x00000003
     *        PUBLIC          0x00000000
     *        X-PERSONAL      0x00000001
     * @return Value of PidTagSensitiviy property
     * @throws IOException
     */
    public Clazz getIcalClass() throws IOException {
        Integer mapiSensitivity = getMapiSensitivity();
        if (mapiSensitivity == null) {
            return Clazz.PUBLIC;
        }
        if (mapiSensitivity == 0) {
            return Clazz.PUBLIC;
        } else if (mapiSensitivity == 1) {
            // not sure X-PERSONAL will be well supported
            return Clazz.PRIVATE;
        } else if (mapiSensitivity == 2) {
            return Clazz.PRIVATE;
        } else if (mapiSensitivity == 3) {
            return Clazz.CONFIDENTIAL;
        }
        return Clazz.PUBLIC;
    }

    /**
     * Canonical name: PidTagSensitivity - Property ID: 0x0036
     *    MS-OXICAL recommends ICAL Class <--> PidTagSensitivity mapping :
     *        CLASS           PidTagSensitivity
     *        PRIVATE         0x00000002
     *        CONFIDENTIAL    0x00000003
     *        PUBLIC          0x00000000
     *        X-PERSONAL      0x00000001
     * @return Value of PidTagSensitiviy property
     * @throws IOException
     */
    public Integer getMapiSensitivity() throws IOException {
        return getIntegerValue(null, 0x36);
    }

    /**
     * PidLidBusyStatus Specifies the availability of a user for the event
     * described by the object.
     *    MS-OXICAL recommends PidLidBusyStatus <--> ICAL TRANSP mapping :
     *  Mapping to TRANSP : 0x00000000 TRANSPARENT
     *                      0x00000001 OPAQUE
     *                      0x00000002 OPAQUE
     *                      0x00000003 OPAQUE
     * @return Value of PidLidBusyStatus property
     * @throws IOException
     */
    public BusyStatus getBusyStatus() throws IOException {
        Integer intVal = getIntegerValue(PSETID_AppointmentPropName(0x8205), 0);
        if (intVal == null) {
            return null;
        }
        for (BusyStatus curr : BusyStatus.values()) {
            if (curr.mapiPropValue() == intVal) {
                return curr;
            }
        }
        return null;
    }

    /**
     * PidLidIntendedBusyStatus - Contains the value of the PidLidBusyStatus
     * property on the Meeting object in the organizer's calendar at the time
     * the Meeting Request object or Meeting Update object was sent.
     * @return Value of PidLidIntendedBusyStatus property
     * @throws IOException
     */
    public BusyStatus getIntendedBusyStatus() throws IOException {
        Integer intVal = getIntegerValue(PSETID_AppointmentPropName(0x8224), 0);
        if (intVal == null) {
            return null;
        }
        for (BusyStatus curr : BusyStatus.values()) {
            if (curr.mapiPropValue() == intVal) {
                return curr;
            }
        }
        return null;
    }

    /**
     * PidLidAppointmentStateFlags - Specifies a bit field that describes the state of the object.
     * This property is not required. The following are the individual flags that can be set.
     * (asfMeeting, 0x00000001): object is a Meeting object or a meeting related object.
     * (asfReceived, 0x00000002): object was received from someone else.
     * (asfCanceled, 0x00000004): Meeting object that is represented by the object has been cancelled.
     * TODO: MS-OXCICAL says attendee properties SHOULD NOT be exported if asfMeeting not set.
     * @return Value representing known flags in PidLidAppointmentStateFlags property
     * @throws IOException
     */
    public EnumSet <AppointmentStateFlags> getAppointmentStateFlags() throws IOException {
        if (appointmentStateFlagsMask != null) {
            return appointmentStateFlagsMask;
        }
        appointmentStateFlagsMask = EnumSet.noneOf(AppointmentStateFlags.class);
        Integer apptFlags = getIntegerValue(PSETID_AppointmentPropName(0x8217), 0);
        if (apptFlags == null) {
            return null;
        }
        if ( (apptFlags & 0x00000001) == 0x00000001) {
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_MEETING);
        }
        if ( (apptFlags & 0x00000002) == 0x00000002) {
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_MEETING);
        }
        if ( (apptFlags & 0x00000004) == 0x00000004) {
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_MEETING);
        }
        return appointmentStateFlagsMask;
    }

    /**
     * @return value of PidTagResponseRequested property or null if absent
     * @throws IOException
     */
    public Boolean getResponseRequested() throws IOException {
        return getBooleanValue(null, MAPIProp.PR_RESPONSE_REQUESTED);
    }

    /**
     * PidLidAppointmentSubType - Specifies whether the event is an all-day event.
     * @return value of PidLidAppointmentSubType property or false if absent
     * @throws IOException
     */
    public Boolean isAllDayEvent() throws IOException {
        MAPIPropName PidLidAppointmentSubType = PSETID_AppointmentPropName(0x8215);
        return getBooleanValue(PidLidAppointmentSubType, 0, false);
    }

    /**
     * PidLidAppointmentCounterProposal Indicates whether a Meeting Response
     * object is a counter proposal.
     * @return value of PidLidAppointmentCounterProposal or false if absent
     * @throws IOException
     */
    public Boolean isCounterProposal() throws IOException {
        MAPIPropName PidLidAppointmentCounterProposal = PSETID_AppointmentPropName(0x8257);
        return getBooleanValue(PidLidAppointmentCounterProposal, 0, false);
    }

    /**
     * PidLidAppointmentNotAllowPropose - Indicates whether attendees are not
     * allowed to propose a new date and/or time for the meeting.
     * @return value of PidLidAppointmentNotAllowPropose or null
     * @throws IOException
     */
    public Boolean isDisallowCounter() throws IOException {
        MAPIPropName PidLidAppointmentNotAllowPropose
                = PSETID_AppointmentPropName(0x825A);
        return getBooleanValue(PidLidAppointmentNotAllowPropose, 0);
    }

    /**
     * @return value of PidLidLocation property or null if absent
     * @throws IOException
     */
    public String getLocation() throws IOException {
        MAPIPropName PidLidLocation = PSETID_AppointmentPropName(0x8208);
        return getStringValue(PidLidLocation, 0);
    }

    /**
     * @return value of PidNameKeywords property or null if absent
     * @throws IOException
     */
    public String[] getCategories() throws IOException {
        //TODO: Implement this
        MAPIPropName PidNameKeywords = this.PS_PUBLIC_STRINGS_PropName("Keywords");
        RawInputStream ris = getRawInputStreamValue(PidNameKeywords, 0);
        if (ris != null) {
            sLog.debug("getCategories Not yet implemented");
        }
        return null;
    }

    /**
     * PidLidOwnerCriticalChange - Specifies the date and time at which a Meeting
     * Request object was sent by the organizer.
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getOwnerCriticalChange() throws IOException {
        MAPIPropName PidLidOwnerCriticalChange = PSETID_MeetingPropName(0x1a);
        return getUtcDateTime(PidLidOwnerCriticalChange, 0);
    }

    /**
     * PidLidAppointmentReplyTime - Specifies the date and time at which the
     * attendee responded to a received meeting request or Meeting Update object.
     * @return Value of PidLidAppointmentReplyTime -
     * @throws IOException
     */
    public DateTime getAppointmentReplyTime() throws IOException {
        MAPIPropName PidLidAppointmentReplyTime
                = PSETID_AppointmentPropName(0x8220);
        return getUtcDateTime(PidLidAppointmentReplyTime, 0);
    }

    /**
     * PidLidAppointmentStartWhole - Specifies the start date and time for the event.
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getStartTime() throws IOException {
        // Specifies the start date and time of the event in UTC
        MAPIPropName PidLidAppointmentStartWhole
                = PSETID_AppointmentPropName(0x820D);
        DateTime timeVal = getUtcDateTime(PidLidAppointmentStartWhole, 0);
        if (timeVal == null) {
            timeVal = getUtcDateTime(null, MAPIProp.PR_START_DATE);
        }
        return timeVal;
    }

    /**
     * PidLidAppointmentEndWhole - Specifies the end date and time for the event.
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getEndTime() throws IOException {
        MAPIPropName PidLidAppointmentEndWhole
                = PSETID_AppointmentPropName(0x820E);
        DateTime timeVal = getUtcDateTime(PidLidAppointmentEndWhole, 0);
        if (timeVal == null) {
            timeVal = getUtcDateTime(null, MAPIProp.PR_END_DATE);
        }
        return timeVal;
    }

    /**
     * @return PidLidExceptionReplaceTime which specifies UTC date and time
     * within a recurrence pattern that an exception will replace.
     * @throws IOException
     */
    public DateTime getRecurrenceIdTime() throws IOException {
        MAPIPropName PidLidExceptionReplaceTime
        = PSETID_AppointmentPropName(0x8228);
        return getUtcDateTime(PidLidExceptionReplaceTime, 0);
    }

    /**
     * @return PidLidAppointmentProposedStartWhole which specifies the proposed
     *         value for PidLidAppointmentStartWhole for a counter proposal.
     * @throws IOException
     */
    public DateTime getProposedStartTime() throws IOException {
        MAPIPropName PidLidAppointmentProposedStartWhole
        = PSETID_AppointmentPropName(0x8250);
        return getUtcDateTime(PidLidAppointmentProposedStartWhole, 0);
    }

    /**
     * PidLidAppointmentProposedEndWhole - Specifies the proposed
     *         value for PidLidAppointmentEndWhole for a counter proposal.
     * @return 
     * @throws IOException
     */
    public DateTime getProposedEndTime() throws IOException {
        MAPIPropName PidLidAppointmentProposedEndWhole
        = PSETID_AppointmentPropName(0x8251);
        return getUtcDateTime(PidLidAppointmentProposedEndWhole, 0);
    }

    /**
     * PidLidAttendeeCriticalChange - Specifies the date and time at which the meeting-related object was sent.
     * @return the time in UTC that the meeting-related object was sent or null
     * @throws IOException
     */
    public DateTime getAttendeeCriticalChange() throws IOException {
        MAPIPropName PidLidAttendeeCriticalChange = PSETID_MeetingPropName(0x1);
        return getUtcDateTime(PidLidAttendeeCriticalChange, 0);
    }

    /**
     * PidLidAppointmentTimeZoneDefinitionStartDisplay - Specifies time zone information that indicates
     * the time zone of the PidLidAppointmentStartWhole property.
     *
     * @return
     * @throws IOException
     */
    public TimeZoneDefinition getStartDateTimezoneInfo() throws IOException {
        MAPIPropName PidLidAppointmentTimeZoneDefinitionStartDisplay =
                this.PSETID_AppointmentPropName(0x825E);
        RawInputStream tzRis = getRawInputStreamValue(
                PidLidAppointmentTimeZoneDefinitionStartDisplay, 0);
        if (tzRis == null) {
            return null;
        }
        TimeZoneDefinition startTZDef = new TimeZoneDefinition(tzRis);
        return startTZDef;
    }

    /**
     * PidLidAppointmentTimeZoneDefinitionEndDisplay - Specifies time zone information that indicates
     * the time zone of the PidLidAppointmentEndWhole property.
     *
     * @return
     * @throws IOException
     */
    public TimeZoneDefinition getEndDateTimezoneInfo() throws IOException {
        MAPIPropName PidLidAppointmentTimeZoneDefinitionEndDisplay =
                this.PSETID_AppointmentPropName(0x825F);
        RawInputStream tzRis = getRawInputStreamValue(
                PidLidAppointmentTimeZoneDefinitionEndDisplay, 0);
        if (tzRis == null) {
            return null;
        }
        TimeZoneDefinition endTZDef = new TimeZoneDefinition(tzRis);
        return endTZDef;
    }

    /**
     * PidLidAppointmentTimeZoneDefinitionRecur - Specifies time zone information
     * that describes how to convert the meeting date and time on a recurring
     * series to and from UTC.
     *
     * @return
     * @throws IOException
     */
    public TimeZoneDefinition getRecurrenceTimezoneInfo() throws IOException {
        MAPIPropName PidLidAppointmentTimeZoneDefinitionRecur =
                this.PSETID_AppointmentPropName(0x8260);
        RawInputStream tzRis = getRawInputStreamValue(
             PidLidAppointmentTimeZoneDefinitionRecur, 0);
        if (tzRis == null) {
            return null;
        }
        TimeZoneDefinition endTZDef = new TimeZoneDefinition(tzRis);
        return endTZDef;
    }

    /**
     * PidLidTimeZoneStruct Specifies time zone information for a recurring meeting.
     *
     * @return
     * @throws IOException
     */
    public Object getTimeZoneStructInfo() throws IOException {
        MAPIPropName PidLidTimeZoneStruct = this.PSETID_AppointmentPropName(0x8233);
        RawInputStream tzRis = getRawInputStreamValue(PidLidTimeZoneStruct, 0);
        return tzRis;
    }

    /**
     * PidLidTimeZoneDescription Specifies a human-readable description
     * of the time zone that is represented by the data in the
     * PidLidTimeZoneStruct property.
     * @return value of PidLidTimeZoneDescription property or null if absent
     * @throws IOException
     */
    public String getTimeZoneDescription() throws IOException {
        MAPIPropName PidLidTimeZoneDescription = PSETID_AppointmentPropName(0x8234);
        return getStringValue(PidLidTimeZoneDescription, 0);
    }

    public RecurrenceDefinition getRecurrenceDefinition(
            TimeZoneDefinition tzDef) throws IOException {
        MAPIPropName PidLidAppointmentRecur =
                this.PSETID_AppointmentPropName(0x8216);
        RawInputStream tzRis = getRawInputStreamValue(
                PidLidAppointmentRecur, 0);
        if (tzRis == null) {
            return null;
        }
        RecurrenceDefinition recurDef = new RecurrenceDefinition(tzRis, tzDef);
        return recurDef;
    }

    public DateTime getUtcDateTime(MAPIPropName name, int id) throws IOException {
        Date javaDate = getDateValue(name, id);
        if (javaDate == null) {
            return null;
        }
        DateTime icalDateTime = new net.fortuna.ical4j.model.DateTime(javaDate);
        icalDateTime.setUtc(true);
        return icalDateTime;
    }

    public String getStringValue(MAPIPropName name, int id) throws IOException {
        MAPIValue mpValue = getFirstValue(name, id);
        if (mpValue == null) {
            return null;
        }
        Object obj = mpValue.getValue();
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

    public Boolean getBooleanValue(MAPIPropName name, int id, boolean defaultValue) throws IOException {
        Boolean truthValue = getBooleanValue(name, id);
        if (truthValue == null) {
            truthValue = new Boolean(defaultValue);
        }
        return truthValue;
    }

    public Boolean getBooleanValue(MAPIPropName name, int id) throws IOException {
        MAPIValue mpValue = getFirstValue(name, id);
        if (mpValue == null) {
            return null;
        }
        Object obj = mpValue.getValue();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return null;
    }

    public Integer getIntegerValue(MAPIPropName name, int id) throws IOException {
        MAPIValue mpValue = getFirstValue(name, id);
        if (mpValue == null) {
            return null;
        }
        Object obj = mpValue.getValue();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        return null;
    }

    public Date getDateValue(MAPIPropName name, int id) throws IOException {
        MAPIValue mpValue = getFirstValue(name, id);
        if (mpValue == null) {
            return null;
        }
        Object obj = mpValue.getValue();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Date) {
            return (Date) obj;
        }
        return null;
    }

    public RawInputStream getRawInputStreamValue(MAPIPropName name, int id) throws IOException {
        MAPIValue mpValue = getFirstValue(name, id);
        if (mpValue == null) {
            return null;
        }
        Object obj = mpValue.getValue();
        if (obj == null) {
            return null;
        }
        if (obj instanceof RawInputStream) {
            return (RawInputStream) obj;
        }
        return null;
    }

    public byte[] getByteArrayValue(MAPIPropName name, int id) throws IOException {
        MAPIValue mpValue = getFirstValue(name, id);
        if (mpValue == null) {
            return null;
        }
        Object obj = mpValue.getValue();
        if (obj == null) {
            return null;
        }
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        }
        return null;
    }

    public MAPIValue getFirstValue(MAPIPropName name, int id) throws IOException {
        MAPIValue[] mpValues = getValues(name, id);
        if (mpValues == null) {
            return null;
        }
        if (mpValues.length < 1) {
            return null;
        }
        return mpValues[0];
    }

    public MAPIValue[] getValues(MAPIPropName name, int id) throws IOException {
        MAPIProp mp;
        mp = getProp(name, id);
        if (mp == null) {
            return null;
        }
        return mp.getValues();
    }

    public MAPIProp getProp(MAPIPropName name, int id) throws IOException {
        MAPIProp mp;
        List <?> attribs = (List <?>) super.getAttributes();
        for (Object thisObj : attribs) {
            if (! (thisObj instanceof Attr)) {
                continue;
            }
            Attr thisAtt = (Attr) thisObj;
            Object o = thisAtt.getValue();
            if (o == null) {
                continue;
            }
            if (thisAtt.getID() != (Attr.attMAPIProps)) {
                continue;
            }
            if (!(o instanceof MAPIProps)) {
                return null;
            }
            MAPIProps thisPropset = (MAPIProps) o;
            if (name == null) {
                mp = thisPropset.getProp(id);
            } else {
                mp = thisPropset.getProp(name);
            }
            if (mp != null) {
                return mp;
            }
        }
        return null;
    }

    /**
     *  Gets an attribute with the given id.
     *
     * @param attrib is the TNEF attribute ID
     * @return the requested attribute, or null if no such attribute exists
     * @throws IOException
     */
    public Attr getAttr(int attrib) throws IOException {
        List <?> attribs = (List <?>) super.getAttributes();
        for (Object thisObj : attribs) {
            if (! (thisObj instanceof Attr)) {
                continue;
            }
            Attr thisAtt = (Attr) thisObj;
            if (thisAtt.getID() == (attrib)) {
                return thisAtt;
            }
        }
        return null;
    }

    /**
     *
     * @param LID - Property Long ID
     * @return MAPIPropName for set PSETID_Appointment with this Property Long ID
     */
    public MAPIPropName PSETID_AppointmentPropName(int LID) {
        return new MAPIPropName(MSGUID.PSETID_Appointment.getJtnefGuid(), LID);
    }

    /**
     *
     * @param LID - Property Long ID
     * @return MAPIPropName for set PSETID_Meeting with this Property Long ID
     */
    public MAPIPropName PSETID_MeetingPropName(int LID) {
        return new MAPIPropName(MSGUID.PSETID_Meeting.getJtnefGuid(), LID);
    }

    /**
     *
     * @param LID - Property Long ID
     * @return MAPIPropName for set PS_PUBLIC_STRINGS with this Property Long ID
     */
    public MAPIPropName PS_PUBLIC_STRINGS_PropName(int LID) {
        return new MAPIPropName(MSGUID.PS_PUBLIC_STRINGS.getJtnefGuid(), LID);
    }

    /**
     *
     * @param propName - Property Name
     * @return MAPIPropName for set PS_PUBLIC_STRINGS with this Property Name
     */
    public MAPIPropName PS_PUBLIC_STRINGS_PropName(String propName) {
        return new MAPIPropName(MSGUID.PS_PUBLIC_STRINGS.getJtnefGuid(), propName);
    }

    /**
     * Provide an Xml-like representation of the scheduling
     * view of the TNEF Message
     *
     * @return
     * @throws IOException if an I/O error occurs
     */
    public StringBuffer toXmlStringBuffer() throws IOException {
        xmlIndentLevel = 0;
        StringBuffer s = new StringBuffer();
        appendFormattedInfo(s, "<tnef>\n");
        xmlIndentLevel++;
        List <?> attribute = (List <?>) super.getAttributes();
        for (Object thisObj : attribute) {
            if (! (thisObj instanceof Attr)) {
                continue;
            }
            Attr thisAtt = (Attr) thisObj;
            String attrId = TNEFUtils.getConstName(thisAtt.getClass(),"att", thisAtt.getID());
            Object o = thisAtt.getValue();

            if (thisAtt.getID() == (Attr.attMAPIProps)) {
                if (o instanceof MAPIProps) {
                    MAPIProps thisPropset = (MAPIProps) o;
                    appendXmlEquiv(s, thisPropset);
                } else {
                    sLog.debug("Parsing issue - attMAPIProps attribute missing MAPIProps value");
                }
            } else if (o instanceof MAPIProps[]) {
                // e.g. Used for attRecipTable
                MAPIProps[] mpArray = (MAPIProps[]) o;
                appendFormattedInfo(s, "<mapi_prop_array tnef_id=\"%s\" len=\"%s\">\n",
                        attrId, new Integer(mpArray.length));
                xmlIndentLevel++;
                for (int mp = 0; mp < mpArray.length; mp++) {
                    appendXmlEquiv(s, mpArray[mp]);
                }
                xmlIndentLevel--;
                appendFormattedInfo(s, "</mapi_prop_array>\n");
            } else {
                appendFormattedInfo(s, "<tnef_attribute id=\"%s\" type=\"%s\" level=\"%s\" length=\"%s\">\n",
                        attrId,
                        TNEFUtils.getConstName(thisAtt.getClass(),"atp",thisAtt.getType()),
                        thisAtt.getLevel(),
                        thisAtt.getLength());
                xmlIndentLevel++;
                appendFormattedInfo(s, "<value>%s</value>\n", thisAtt.getValue());
                xmlIndentLevel--;
                appendFormattedInfo(s, "</tnef_attribute>\n");
            }
        }
        xmlIndentLevel--;
        appendFormattedInfo(s, "</tnef>\n");
        return (s);
    }

    private void appendXmlEquiv(StringBuffer s, MAPIProps thisPropset) throws IOException {
        MAPIProp props[] = thisPropset.getProps();
        if (props.length > 1) {
            appendFormattedInfo(s, "<mapi_prop_list len=\"%s\">\n", new Integer(props.length));
            xmlIndentLevel++;
        }
        for (MAPIProp mp : props) {
            appendXmlEquiv(s, mp);
        }
        if (props.length > 1) {
            xmlIndentLevel--;
            appendFormattedInfo(s, "</mapi_prop_list>\n", new Integer(props.length));
        }
    }

    private void appendXmlEquiv(StringBuffer s, MAPIProp mp) throws IOException {
        appendFormattedInfo(s, "<mapiprop>\n");
        xmlIndentLevel++;
        appendFormattedInfo(s, "<type>%s</type>\n",
                TNEFUtils.getConstName(mp.getClass(),"PT_",mp.getType()));
        MAPIPropName pName = mp.getName();
        if (pName != null) {
            GUID guid = pName.getGUID();
            MSGUID msGuid = new MSGUID(guid);
            appendFormattedInfo(s, "<name>\n");
            xmlIndentLevel++;
            appendFormattedInfo(s, "<guid>%s</guid>\n", msGuid);
            String idName = pName.getName();
            if (idName != null) {
                appendFormattedInfo(s, "<idname>%s</idname>\n", idName);
            }
            StringBuffer nameIdHex = new StringBuffer("0x");
            nameIdHex.append(Long.toHexString(pName.getID()));
            appendFormattedInfo(s, "<lid>%s</lid>\n", nameIdHex);
            xmlIndentLevel--;
            appendFormattedInfo(s, "</name>\n");
        }
        StringBuffer idHex = new StringBuffer("0x");
        idHex.append(Integer.toHexString(mp.getID()));
        String constName = TNEFUtils.getConstName(mp.getClass(),"PR_", mp.getID());
        if (constName.equals(idHex.toString())) {
            appendFormattedInfo(s, "<id>%s</id>\n", constName);
        } else {
            appendFormattedInfo(s, "<id define=\"%s\">%s</id>\n", constName, idHex);
        }
        if (mp.getLength() == 0) {
            appendFormattedInfo(s, "<value></value>\n");
        } else if (mp.getLength() == 1) {
            appendFormattedInfo(s, "<value>%s</value>\n", mp.getValues()[0]);
        }
        else {
            appendFormattedInfo(s, "<values>\n");
            xmlIndentLevel++;
            for (MAPIValue mapiVal : mp.getValues()) {
                appendFormattedInfo(s, "<value>%s</value>\n", mapiVal);
            }
            xmlIndentLevel--;
            appendFormattedInfo(s, "</values>\n");
        }
        xmlIndentLevel--;
        appendFormattedInfo(s, "</mapiprop>\n");
    }

	/**
	 * @param s
	 * @param format
	 * @param objects
	 */
    private void appendFormattedInfo(StringBuffer s, String format, Object ... objects) {
        for (int i = 0; i < xmlIndentLevel; ++i) {
            s.append("  ");
        }
        s.append(String.format(format, objects));
    }

}
