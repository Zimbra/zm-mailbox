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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zimbra.cs.util.tnef.mapi.GlobalObjectId;
import com.zimbra.cs.util.tnef.mapi.RecurrenceDefinition;
import com.zimbra.cs.util.tnef.mapi.TZRule;
import com.zimbra.cs.util.tnef.mapi.TimeZoneDefinition;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.Clazz;
import net.freeutils.tnef.Attachment;
import net.freeutils.tnef.Attr;
import net.freeutils.tnef.CompressedRTFInputStream;
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

    private String messageClass;
    private GlobalObjectId globalObjectId;
    private GlobalObjectId cleanGlobalObjectId;
    private boolean tzinfoInitialized;
    private TimeZoneDefinition startTimeTZinfo;
    private TimeZoneDefinition endTimeTZinfo;
    private TimeZoneDefinition recurrenceTZinfo;
    
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
        tzinfoInitialized = false;
        startTimeTZinfo = null;
        endTimeTZinfo = null;
        recurrenceTZinfo = null;
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
        Attachment attachmnt = null;
        while ((attr = in.readAttr()) != null) {
            switch (attr.getLevel()) {
                case Attr.LVL_ATTACHMENT:
                    switch (attr.getID()) {
                        case Attr.attAttachRenddata:
                            if (attachmnt != null) {
                                super.addAttachment(attachmnt);
                            }
                            attachmnt = new Attachment();
                            attachmnt.addAttribute(attr);
                            break;
                        case Attr.attAttachment:
                            MAPIProps mps = new MAPIProps((RawInputStream)attr.getValue());
                            attachmnt.setMAPIProps(mps);
                            break;
                        case Attr.attAttachData:
                            RawInputStream ris = (RawInputStream)attr.getValue();
                            attachmnt.setRawData(ris);
                            break;
                        case Attr.attAttachTransportFilename:
                            RawInputStream data = (RawInputStream)attr.getValue();
                            String filename = TNEFUtils.removeTerminatingNulls(
                                    new String(data.toByteArray(), super.getOEMCodePage())
                                    );
                            attachmnt.setFilename(filename);
                            break;
                        default:
                            attachmnt.addAttribute(attr);
                            break;
                    } // switching on ID for LVL_ATTACHMENT
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

        if (attachmnt != null) {
            super.addAttachment(attachmnt);
        }
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
        getGlobalObjectId();
        if (globalObjectId != null) {
            return globalObjectId.getIcalUid();
        }
        getCleanGlobalObjectId();
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
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_RECEIVED);
        }
        if ( (apptFlags & 0x00000004) == 0x00000004) {
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_CANCELED);
        }
        return appointmentStateFlagsMask;
    }

    /**
     * @return whether responses are desired for this calendaring object or not
     * @throws IOException
     */
    public boolean getResponseRequested() throws IOException {
        Boolean responseRequested = getBooleanValue(null, MAPIProp.PR_RESPONSE_REQUESTED);
        if (responseRequested != null) {
            return responseRequested;
        }
        Boolean replyRequested = getBooleanValue(null, MAPIProp.PR_REPLY_REQUESTED);
        if (replyRequested != null) {
            return replyRequested;
        }
        return false;
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
    public List <String> getCategories() throws IOException {
        //TODO: Implement this
        MAPIPropName PidNameKeywords = this.PS_PUBLIC_STRINGS_PropName("Keywords");
        MAPIValue[] values = getValues(PidNameKeywords, 0);
        if (values == null) {
            return null;
        }
        ArrayList <String> categories = new ArrayList <String> ();
        for (MAPIValue val:values) {
            categories.add(val.toString());
        }
        return categories;
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
     * PidLidReminderDelta is the Time before the start of an appointment in minutes
     * at which an alarm should be raised.
     * If PidLidReminderDelta is set to 0x5AE980E1, this is a synonym for
     * 15 minutes before the start.
     *
     * @return
     * @throws IOException
     */
    public Integer getReminderDelta() throws IOException {
        MAPIPropName PidLidReminderDelta
                = PSETID_CommonPropName(0x8501);
        Integer reminderDelta = this.getIntegerValue(PidLidReminderDelta, 0);
        if ( (reminderDelta != null) && (reminderDelta == 0x5AE980E1) ) {
            reminderDelta= 15;
        }
        return reminderDelta;
    }

    /**
     * 
     * @return value of PidLidReminderSet property which specifies whether
     * a reminder is set on the object.
     * @throws IOException
     */
    public boolean getReminderSet() throws IOException {
        // Specifies the start date and time of the event in UTC
        MAPIPropName PidLidReminderSet
                = PSETID_CommonPropName(0x8503);
        return this.getBooleanValue(PidLidReminderSet, 0, false);
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
     * Candidate properties :
     *
     * PidLidExceptionReplaceTime specifies UTC date and time
     * within a recurrence pattern that an exception will replace.
     *
     * @return A suitable time for the RECURRENCE-ID property for this
     * calendaring item if one exists.
     * @throws IOException
     */
    public DateTime getRecurrenceIdTime() throws IOException {
        // Note: Was assuming PidLidOldWhenStartWhole was a good candidate
        //       but that seems to be used for non-recurrence related objects
        //       to give the previous start time of a single appointment.
        MAPIPropName PidLidExceptionReplaceTime
                = PSETID_AppointmentPropName(0x8228);
        DateTime recurrenceIdTime = getUtcDateTime(PidLidExceptionReplaceTime, 0);
        if (recurrenceIdTime != null) {
            sLog.debug("RECURRENCE-ID taken from PidLidExceptionReplaceTime");
        } else {
            getGlobalObjectId();
            if (this.globalObjectId == null) {
                return null;
            }
            int origYear = globalObjectId.getOrigInstanceYear();
            if (origYear == 0) {
                return null;
            }
            int origMonth = globalObjectId.getOrigInstanceMonth();
            int origDay = globalObjectId.getOrigInstanceDay();
            // PidLidStartRecurrenceTime identifies start time of the recurrence pattern.
            MAPIPropName pnPidLidStartRecurrenceTime = PSETID_MeetingPropName(0xe);
            Integer recurTime = this.getIntegerValue(pnPidLidStartRecurrenceTime, 0);
            int secs = 0;
            int mins = 0;
            int hrs = 0;
            if (recurTime != null) {
                // bottom 6 bits are seconds, next 6 bits are minutes, rest is hours
                secs = recurTime & 0x0000003f;
                mins = (recurTime & 0x00000fc0) >>6;
                hrs = (recurTime & 0xfffff000) >>12;
                sLog.debug("RECURRENCE-ID taken from GlobalObjectId/PidLidStartRecurrenceTime");
            } else {
                sLog.debug("RECURRENCE-ID taken from JUST GlobalObjectId");
            }
            try {
                String dateString;
                if (recurrenceTZinfo == null) {
                    dateString = String.format("%04d%02d%02dT%02d%02d%02dZ",
                        origYear, origMonth, origDay, hrs, mins, secs);
                    recurrenceIdTime = new DateTime(dateString);
                } else {
                    dateString = String.format("%04d%02d%02dT%02d%02d%02d",
                        origYear, origMonth, origDay, hrs, mins, secs);
                    recurrenceIdTime = new DateTime(dateString, recurrenceTZinfo.getTimeZone());
                }
            } catch (ParseException e) {
                sLog.debug("Problem constructing recurrence-id time", e);
            }
        }
        return recurrenceIdTime;
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
     * @return TimeZone information relevant to the start of an appointment.
     * @throws IOException
     */
    public TimeZoneDefinition getStartDateTimezoneInfo() throws IOException {
        initTZinfo();
        return startTimeTZinfo;
    }

    /**
     * @return TimeZone information relevant to the end of an appointment.
     * @throws IOException
     */
    public TimeZoneDefinition getEndDateTimezoneInfo() throws IOException {
        initTZinfo();
        return endTimeTZinfo;
    }

    /**
     * @return TimeZone information relevant to the rule of a recurrence.
     * @throws IOException
     */
    public TimeZoneDefinition getRecurrenceTimezoneInfo() throws IOException {
        initTZinfo();
        return recurrenceTZinfo;
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
        String oemCodePage = super.getOEMCodePage();
        RecurrenceDefinition recurDef = new RecurrenceDefinition(tzRis, tzDef, oemCodePage);
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

    /**
     * Useful for X-ALT-DESC?  Would need to convert to HTML, which, even for wrapped HTML
     * which isn't necessarily common place, isn't 100% straight forward.
     * 
     * @return
     * @throws IOException
     */
    public String getRTF() throws IOException {
        RawInputStream ris = getRawInputStreamValue(null, MAPIProp.PR_RTF_COMPRESSED);
        if (ris == null) {
            sLog.debug("No PR_RTF_COMPRESSED property found");
            return null;
        }
        // TODO:  What impact does the OEMCodePage for the TNEF have on this?
        String rtfTxt = new String(
                CompressedRTFInputStream.decompressRTF(
                        ris.toByteArray()));
        if (sLog.isDebugEnabled()) {
            sLog.debug("RTF from PR_RTF_COMPRESSED\n%s\n", rtfTxt);
        }
        return rtfTxt;
    }

    public String getStringValue(MAPIPropName name, int id) throws IOException {
        MAPIValue mpValue = getFirstValue(name, id);
        if (mpValue == null) {
            return null;
        }
        Object obj;
        if (mpValue.getType() == MAPIProp.PT_STRING) {
            // Assume the value is in the OEM Code Page
            // The current MAPIValue code does not take account of that.
            RawInputStream ris = mpValue.getRawData();
            return IcalUtil.readString(ris, (int)ris.getLength(),
                        super.getOEMCodePage());
        } else {
            // Probably PT_UNICODE_STRING but will accept anything whose value is
            // a String.
            obj = mpValue.getValue();
        }
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
     * @param LID - Property Long ID
     * @return MAPIPropName for set PSETID_Common with this Property Long ID
     */
    public MAPIPropName PSETID_CommonPropName(int LID) {
        return new MAPIPropName(MSGUID.PSETID_Common.getJtnefGuid(), LID);
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
     * PidLidGlobalObjectId is The unique identifier of the Calendar object.
     * After it is set for a Calendar object, the value of this property MUST NOT change.
     * 
     * @return value of PidLidGlobalObjectId property
     */
    private GlobalObjectId getGlobalObjectId() {
        if (globalObjectId != null) {
            return globalObjectId;
        }
        MAPIPropName pnPidLidGlobalObjectId = new MAPIPropName(MSGUID.PSETID_Meeting.getJtnefGuid(), (long)0x3);
        globalObjectId = this.getGlobalObjectIdType(pnPidLidGlobalObjectId);
        return globalObjectId;
    }

    /**
     * PidLidCleanGlobalObjectId
     * The format of this property is the same as that of PidLidGlobalObjectId.
     * The value of this property MUST be equal to the value of PidLidGlobalObjectId,
     * except the YH, YL, M, and D fields MUST all be zero.
     * All objects that refer to an instance of a recurring series (including
     * an orphan instance), as well as the recurring series itself, will have the
     * same value for this property.
     * 
     * @return value of PidLidCleanGlobalObjectId property
     */
    private GlobalObjectId getCleanGlobalObjectId() {
        if (cleanGlobalObjectId != null) {
            return cleanGlobalObjectId;
        }
        MAPIPropName pnPidCleanGlobalObjectId = new MAPIPropName(MSGUID.PSETID_Meeting.getJtnefGuid(), (long)0x23);
        cleanGlobalObjectId = this.getGlobalObjectIdType(pnPidCleanGlobalObjectId);
        return cleanGlobalObjectId;
    }

    private GlobalObjectId getGlobalObjectIdType(MAPIPropName pName) {
        GlobalObjectId gid = null;
        RawInputStream ris;
        try {
            ris = getRawInputStreamValue(pName, 0);
            if (ris != null) {
                gid = new GlobalObjectId(ris);
            }
        } catch (IOException e) {
            sLog.debug("Problem getting value of MAPI property " + pName + " from TNEF", e);
        }
        return gid;
    }

    /**
     * For a Task, PidLidTaskGlobalId contains a unique key.
     * 
     * @return 
     * @throws IOException 
     */
    private String getPidLidTaskGlobalId() throws IOException {
        MAPIPropName pnPidLidTaskGlobalId = new MAPIPropName(MSGUID.PSETID_Common.getJtnefGuid(), (long)0x8519);
        byte[] theVal = this.getByteArrayValue(pnPidLidTaskGlobalId, 0);
        return null;
    }

    /**
     * Initialise timezone related fields.
     * Replies from Outlook 2007 related to a recurrence or an instance
     * of a recurrence had :
     *     PidLidTimeZoneDescription,PidLidTimeZoneStruct,
     *     PidLidAppointmentTimeZoneDefinitionStartDisplay and
     *     PidLidAppointmentTimeZoneDefinitionEndDisplay present but NOT
     *     PidLidAppointmentTimeZoneDefinitionRecur
     * further, the StartDisplay/EndDisplay properties were appropriate to the
     * Outlook client replying and were NOT related to the originally sent ICAL.
     *
     * The Outlook originated TimeZone names associated with
     * StartDisplay/EndDisplay/Recur props seem "nicer" than the names
     * used in PidLidTimeZoneDescription - so, tend to prefer those.
     *
     * @throws IOException
     */
    private void initTZinfo() {
        if (tzinfoInitialized) {
            return;
        }

        try {
            RawInputStream tzRis;
            MAPIPropName mpn;
            mpn = TimeZoneDefinition.PidLidAppointmentTimeZoneDefinitionStartDisplay;
            tzRis = getRawInputStreamValue(mpn, 0);
            if (tzRis != null) {
                startTimeTZinfo = new TimeZoneDefinition(mpn, tzRis);
            }
            mpn = TimeZoneDefinition.PidLidAppointmentTimeZoneDefinitionEndDisplay;
            tzRis = getRawInputStreamValue(mpn, 0);
            if (tzRis != null) {
                endTimeTZinfo = new TimeZoneDefinition(mpn, tzRis);
            }
            mpn = TimeZoneDefinition.PidLidAppointmentTimeZoneDefinitionRecur;
            tzRis = getRawInputStreamValue(mpn, 0);
            if (tzRis != null) {
                recurrenceTZinfo = new TimeZoneDefinition(mpn, tzRis);
            }
            String tzDesc = this.getTimeZoneDescription();
            if (null != tzDesc) {
                TimeZoneDefinition tzStructInfo = this.getTimeZoneStructInfo(tzDesc);
                if (tzStructInfo != null) {
                    // We know we have a recurrence related TZ definition.  Make
                    // sure we have the most appropriate/nice definition for that.
                    TZRule tzsRule = tzStructInfo.getEffectiveRule();
                    if (recurrenceTZinfo == null) {
                        if  (   (startTimeTZinfo != null) &&
                                (tzsRule.equivalentRule(
                                        startTimeTZinfo.getEffectiveRule())) ) {
                            recurrenceTZinfo = startTimeTZinfo;
                            sLog.debug("Using %s for TZ info",
                                    "PidLidAppointmentTimeZoneDefinitionStart");
                        } else if  (   (endTimeTZinfo != null) &&
                                (tzsRule.equivalentRule(
                                        endTimeTZinfo.getEffectiveRule())) ) {
                            recurrenceTZinfo = endTimeTZinfo;
                            sLog.debug("Using %s for TZ info",
                                    "PidLidAppointmentTimeZoneDefinitionEnd");
                        } else {
                            recurrenceTZinfo = tzStructInfo;
                            sLog.debug("Using %s for TZ info",
                                    "PidLidTimeZoneStruct");
                        }
                    } else if (!tzsRule.equivalentRule(
                                    recurrenceTZinfo.getEffectiveRule())) {
                        recurrenceTZinfo = tzStructInfo;
                        sLog.debug("Using %s for TZ info",
                                "PidLidAppointmentTimeZoneDefinitionRecur");
                    }
                }
            }
        } catch (IOException e) {
            sLog.debug("Problem encountered initialising timezone information", e);
        }

        if (recurrenceTZinfo != null) {
            // For recurrences, we want just one TZ for consistency
            if (endTimeTZinfo == null) {
                endTimeTZinfo = recurrenceTZinfo;
            } else if (recurrenceTZinfo.getEffectiveRule().equivalentRule(
                            endTimeTZinfo.getEffectiveRule())) {
                endTimeTZinfo = recurrenceTZinfo;
            } else if (startTimeTZinfo != null) {
                // Sometimes, the start and end timezones are different
                // for cancel/request, even when related to an exception.
                if (startTimeTZinfo.getEffectiveRule().equivalentRule(
                            endTimeTZinfo.getEffectiveRule())) {
                    endTimeTZinfo = recurrenceTZinfo;
                }
            }
            startTimeTZinfo = recurrenceTZinfo;
        }
        if (endTimeTZinfo == null) {
            endTimeTZinfo = startTimeTZinfo;
        } else if (startTimeTZinfo == null) {
            startTimeTZinfo = endTimeTZinfo ;
        } else if (startTimeTZinfo.getEffectiveRule().equivalentRule(
                            endTimeTZinfo.getEffectiveRule())) {
            endTimeTZinfo = startTimeTZinfo;
        }
    }

    /**
     * PidLidTimeZoneStruct Specifies time zone information for a recurring meeting.
     *
     * @param tzName 
     * @return
     * @throws IOException
     */
    private TimeZoneDefinition getTimeZoneStructInfo(String tzName) throws IOException {
        MAPIPropName PidLidTimeZoneStruct = this.PSETID_AppointmentPropName(0x8233);
        RawInputStream tzRis = getRawInputStreamValue(PidLidTimeZoneStruct, 0);
        if (tzRis == null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("No PidLidTimeZoneStruct property found");
            }
            return null;
        }
        TimeZoneDefinition tzDef = new TimeZoneDefinition(tzRis, tzName);
        if (tzDef == null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Failed to load TimeZoneDefinition from PidLidTimeZoneStruct and " + tzName);
            }
        }
        return tzDef;
    }

}
